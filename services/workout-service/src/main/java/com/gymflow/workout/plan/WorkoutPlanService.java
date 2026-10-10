package com.gymflow.workout.plan;

import com.gymflow.workout.integration.assistant.AssistantSuggestionClient;
import com.gymflow.workout.integration.gym.GymCatalogClient;
import com.gymflow.workout.shared.error.ConflictException;
import com.gymflow.workout.shared.error.DependencyUnavailableException;
import com.gymflow.workout.shared.error.EligibilityConflictException;
import com.gymflow.workout.shared.error.InvalidRequestException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class WorkoutPlanService {
    private final WorkoutPlanTransactions transactions;
    private final GymCatalogClient catalog;
    private final WorkoutPlanValidator validator;
    private final AssistantSuggestionClient suggestions;
    private final WorkoutPlanMapper mapper;
    private final SuggestionApplier suggestionApplier;

    public WorkoutPlanService(WorkoutPlanTransactions transactions, GymCatalogClient catalog,
                              WorkoutPlanValidator validator, AssistantSuggestionClient suggestions,
                              WorkoutPlanMapper mapper, SuggestionApplier suggestionApplier) {
        this.transactions = transactions;
        this.catalog = catalog;
        this.validator = validator;
        this.suggestions = suggestions;
        this.mapper = mapper;
        this.suggestionApplier = suggestionApplier;
    }

    public PlanModels.PlanResponse create(String subject, PlanModels.CreatePlanRequest request, String token) {
        catalog.validateUnit(request.unitId(), token);
        return transactions.create(subject, request);
    }

    public List<PlanModels.PlanSummary> list(String subject) {
        return transactions.list(subject);
    }

    public PlanModels.PlanResponse get(String subject, UUID id, String token) {
        PlanModels.PlanResponse plan = transactions.owned(subject, id);
        Set<UUID> ids = exerciseIds(plan.days());
        if (ids.isEmpty()) return withEligibility(plan, true, inventoryIssues(plan, List.of()));
        try {
            List<UUID> invalid = invalidIds(catalog.validate(plan.unitId(), ids, token));
            List<PlanModels.PlanIssue> issues = invalid.isEmpty() ? List.of() : List.of(new PlanModels.PlanIssue(
                "EXERCISE_INELIGIBLE", "Há exercícios indisponíveis nesta unidade; ajuste o rascunho antes de ativar",
                invalid));
            return withEligibility(plan, true, inventoryIssues(plan, issues));
        } catch (DependencyUnavailableException exception) {
            return withEligibility(plan, false, inventoryIssues(plan, List.of(new PlanModels.PlanIssue(
                "ELIGIBILITY_UNAVAILABLE", "A elegibilidade atual não pôde ser consultada", List.of()))));
        }
    }

    public PlanModels.PlanResponse update(String subject, UUID id, PlanModels.UpdatePlanRequest request, String token) {
        PlanModels.PlanResponse current = transactions.owned(subject, id);
        requireVersion(current, request.version());
        requireStatus(current, PlanStatus.DRAFT, "Somente planos em rascunho podem ser editados");
        if (!current.unitId().equals(request.unitId())) {
            throw new InvalidRequestException(
                "A unidade do plano é imutável; crie outro plano para treinar em uma unidade diferente");
        }
        validator.validateStructure(request.days(), false);
        if (request.days().size() > request.targetDaysPerWeek()) {
            throw new InvalidRequestException("O rascunho possui mais dias do que a frequência semanal definida");
        }
        validateEligibility(request.unitId(), request.days(), token);
        return transactions.update(subject, id, request, mapper.entities(request.days()));
    }

    public PlanModels.PlanResponse activate(String subject, UUID id, long expectedVersion, String token) {
        PlanModels.PlanResponse current = transactions.owned(subject, id);
        requireVersion(current, expectedVersion);
        if (current.status() == PlanStatus.ARCHIVED) throw new ConflictException("Um plano arquivado não pode ser ativado");
        requireCompleteWeek(current);
        validateEligibility(current.unitId(), requests(current.days()), token);
        return transactions.activate(subject, id, expectedVersion);
    }

    public PlanModels.PlanResponse revalidate(String subject, UUID id, long expectedVersion, String token) {
        PlanModels.PlanResponse current = transactions.owned(subject, id);
        requireVersion(current, expectedVersion);
        requireStatus(current, PlanStatus.ACTIVE, "Somente planos ativos podem ser revalidados");
        requireCompleteWeek(current);
        validateEligibility(current.unitId(), requests(current.days()), token);
        return transactions.revalidate(subject, id, expectedVersion);
    }

    public PlanModels.PlanResponse archive(String subject, UUID id, long expectedVersion) {
        return transactions.archive(subject, id, expectedVersion);
    }

    public PlanModels.PlanResponse applySuggestion(String subject, UUID id, PlanModels.ApplySuggestionRequest request,
                                                    String idempotencyKey, String token) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new InvalidRequestException("Idempotency-Key é obrigatório e deve ter até 100 caracteres");
        }
        if (transactions.previousApplication(subject, idempotencyKey, id, request.suggestionId()).isPresent()) {
            return get(subject, id, token);
        }
        PlanModels.PlanResponse current = transactions.owned(subject, id);
        requireVersion(current, request.expectedPlanVersion());
        requireStatus(current, PlanStatus.DRAFT, "Somente planos em rascunho aceitam sugestões");

        AssistantSuggestionClient.Suggestion suggestion = suggestions.get(request.suggestionId(), token);
        if (!suggestion.planId().equals(id) || suggestion.basePlanVersion() != current.version()) {
            throw new ConflictException("O plano mudou desde a análise; solicite uma nova sugestão");
        }
        if (!"AVAILABLE".equals(suggestion.status()) || suggestion.expiresAt().isBefore(Instant.now())) {
            throw new ConflictException("A sugestão expirou ou não está mais disponível");
        }
        List<PlanModels.DayRequest> changed = suggestionApplier.apply(requests(current.days()), suggestion,
            current.targetDaysPerWeek());
        validator.validateStructure(changed, false);
        validateEligibility(current.unitId(), changed, token);
        return transactions.applySuggestion(subject, id, request.expectedPlanVersion(), idempotencyKey,
            suggestion.id(), mapper.entities(changed));
    }

    private void requireCompleteWeek(PlanModels.PlanResponse plan) {
        List<PlanModels.DayRequest> days = requests(plan.days());
        if (days.size() != plan.targetDaysPerWeek()) {
            throw new InvalidRequestException(
                "Complete os " + plan.targetDaysPerWeek() + " dias definidos antes de ativar o plano");
        }
        validator.validateStructure(days, true);
    }

    private void validateEligibility(UUID unitId, List<PlanModels.DayRequest> days, String token) {
        Set<UUID> exerciseIds = days.stream().flatMap(day -> day.items().stream())
            .map(PlanModels.ItemRequest::exerciseId).collect(Collectors.toSet());
        if (exerciseIds.isEmpty()) return;
        GymCatalogClient.EligibilityResult result = catalog.validate(unitId, exerciseIds, token);
        List<UUID> invalid = invalidIds(result);
        if (!invalid.isEmpty()) {
            throw new EligibilityConflictException(
                "Um ou mais exercícios não estão disponíveis na unidade selecionada", invalid);
        }
        validator.validateKinds(days, result);
    }

    private List<UUID> invalidIds(GymCatalogClient.EligibilityResult result) {
        return result.results().stream().filter(item -> !item.eligible())
            .map(GymCatalogClient.EligibilityItem::exerciseId).toList();
    }

    private List<PlanModels.PlanIssue> inventoryIssues(PlanModels.PlanResponse plan,
                                                        List<PlanModels.PlanIssue> issues) {
        if (!plan.inventoryRevalidationRequired()) return issues;
        List<PlanModels.PlanIssue> result = new ArrayList<>(issues);
        result.add(new PlanModels.PlanIssue("INVENTORY_CHANGED",
            "O inventário da unidade mudou; revise a disponibilidade antes da próxima alteração", List.of()));
        return List.copyOf(result);
    }

    private PlanModels.PlanResponse withEligibility(PlanModels.PlanResponse plan, boolean available,
                                                     List<PlanModels.PlanIssue> issues) {
        return new PlanModels.PlanResponse(plan.id(), plan.unitId(), plan.name(), plan.goal(),
            plan.targetDaysPerWeek(), plan.status(), plan.version(), plan.days(),
            plan.inventoryRevalidationRequired(), available, issues, plan.createdAt(), plan.updatedAt());
    }

    private List<PlanModels.DayRequest> requests(List<PlanModels.DayResponse> days) {
        return days.stream().map(day -> new PlanModels.DayRequest(day.id(), day.position(), day.name(),
            day.items().stream().map(item -> new PlanModels.ItemRequest(item.id(), item.exerciseId(), item.position(),
                item.sets(), item.repetitionMin(), item.repetitionMax(), item.durationSeconds(), item.restSeconds(),
                item.optionalLoadKg(), item.notes())).toList())).toList();
    }

    private Set<UUID> exerciseIds(List<PlanModels.DayResponse> days) {
        return days.stream().flatMap(day -> day.items().stream()).map(PlanModels.ItemResponse::exerciseId)
            .collect(Collectors.toSet());
    }

    private void requireVersion(PlanModels.PlanResponse plan, long expected) {
        if (plan.version() != expected) {
            throw new ConflictException("O plano foi alterado em outra sessão; recarregue antes de salvar");
        }
    }

    private void requireStatus(PlanModels.PlanResponse plan, PlanStatus expected, String message) {
        if (plan.status() != expected) throw new ConflictException(message);
    }
}
