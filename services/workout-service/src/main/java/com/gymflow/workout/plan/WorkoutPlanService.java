package com.gymflow.workout.plan;

import com.gymflow.workout.integration.gym.GymCatalogClient;
import com.gymflow.workout.integration.assistant.AssistantSuggestionClient;
import com.gymflow.workout.suggestion.SuggestionApplication;
import com.gymflow.workout.suggestion.SuggestionApplicationRepository;
import com.gymflow.workout.shared.error.ConflictException;
import com.gymflow.workout.shared.error.DependencyUnavailableException;
import com.gymflow.workout.shared.error.EligibilityConflictException;
import com.gymflow.workout.shared.error.ResourceNotFoundException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutPlanService {
    private final WorkoutPlanRepository repository;
    private final GymCatalogClient catalog;
    private final WorkoutPlanValidator validator;
    private final AssistantSuggestionClient suggestions;
    private final SuggestionApplicationRepository applications;

    public WorkoutPlanService(WorkoutPlanRepository repository, GymCatalogClient catalog, WorkoutPlanValidator validator,
                              AssistantSuggestionClient suggestions, SuggestionApplicationRepository applications) {
        this.repository = repository; this.catalog = catalog; this.validator = validator;
        this.suggestions = suggestions; this.applications = applications;
    }

    @Transactional
    public PlanModels.PlanResponse create(String subject, PlanModels.CreatePlanRequest request) {
        WorkoutPlan plan = repository.save(new WorkoutPlan(subject, request.unitId(), request.name()));
        return response(plan, true, List.of());
    }

    @Transactional(readOnly = true)
    public List<PlanModels.PlanSummary> list(String subject) {
        return repository.findByIdentitySubjectOrderByUpdatedAtDesc(subject).stream()
            .map(plan -> new PlanModels.PlanSummary(plan.getId(), plan.getUnitId(), plan.getName(), plan.getStatus(),
                plan.getVersion(), plan.getDays().size(), plan.isInventoryRevalidationRequired(), plan.getUpdatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public PlanModels.PlanResponse get(String subject, UUID id, String token) {
        WorkoutPlan plan = owned(subject, id);
        Set<UUID> ids = exerciseIds(plan);
        if (ids.isEmpty()) return response(plan, true, inventoryIssues(plan, List.of()));
        try {
            GymCatalogClient.EligibilityResult result = catalog.validate(plan.getUnitId(), ids, token);
            List<UUID> invalid = invalidIds(result);
            List<PlanModels.PlanIssue> issues = invalid.isEmpty() ? List.of() : List.of(new PlanModels.PlanIssue(
                "EXERCISE_INELIGIBLE", "Há exercícios indisponíveis nesta unidade; ajuste o rascunho antes de ativar", invalid));
            return response(plan, true, inventoryIssues(plan, issues));
        } catch (DependencyUnavailableException exception) {
            return response(plan, false, inventoryIssues(plan, List.of(new PlanModels.PlanIssue("ELIGIBILITY_UNAVAILABLE",
                "A elegibilidade atual não pôde ser consultada", List.of()))));
        }
    }

    @Transactional
    public PlanModels.PlanResponse update(String subject, UUID id, PlanModels.UpdatePlanRequest request, String token) {
        WorkoutPlan plan = owned(subject, id);
        requireVersion(plan, request.version());
        if (plan.getStatus() != PlanStatus.DRAFT) throw new ConflictException("Somente planos em rascunho podem ser editados");
        validator.validateStructure(request.days(), false);
        validateEligibility(request.unitId(), request.days(), token);
        List<WorkoutDay> days = request.days().stream().map(this::toEntity).toList();
        plan.replace(request.unitId(), request.name(), days);
        plan.confirmInventoryRevalidation();
        repository.saveAndFlush(plan);
        return response(plan, true, List.of());
    }

    @Transactional
    public PlanModels.PlanResponse activate(String subject, UUID id, long expectedVersion, String token) {
        WorkoutPlan plan = owned(subject, id);
        requireVersion(plan, expectedVersion);
        if (plan.getStatus() == PlanStatus.ARCHIVED) throw new ConflictException("Um plano arquivado não pode ser ativado");
        List<PlanModels.DayRequest> days = requestDays(plan);
        validator.validateStructure(days, true);
        validateEligibility(plan.getUnitId(), days, token);
        plan.confirmInventoryRevalidation();
        plan.activate(); repository.saveAndFlush(plan);
        return response(plan, true, List.of());
    }

    @Transactional
    public PlanModels.PlanResponse archive(String subject, UUID id, long expectedVersion) {
        WorkoutPlan plan = owned(subject, id);
        requireVersion(plan, expectedVersion);
        plan.archive(); repository.saveAndFlush(plan);
        return response(plan, true, List.of());
    }

    @Transactional
    public PlanModels.PlanResponse applySuggestion(String subject, UUID id, PlanModels.ApplySuggestionRequest request,
                                                    String idempotencyKey, String token) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new com.gymflow.workout.shared.error.InvalidRequestException("Idempotency-Key é obrigatório e deve ter até 100 caracteres");
        }
        var previous = applications.findByIdentitySubjectAndIdempotencyKey(subject, idempotencyKey);
        if (previous.isPresent()) {
            SuggestionApplication application = previous.get();
            if (!application.getPlanId().equals(id) || !application.getSuggestionId().equals(request.suggestionId())) {
                throw new ConflictException("A Idempotency-Key já foi usada para outra operação");
            }
            return get(subject, id, token);
        }
        WorkoutPlan plan = owned(subject, id);
        requireVersion(plan, request.expectedPlanVersion());
        if (plan.getStatus() != PlanStatus.DRAFT) throw new ConflictException("Somente planos em rascunho aceitam sugestões");
        AssistantSuggestionClient.Suggestion suggestion = suggestions.get(request.suggestionId(), token);
        if (!suggestion.planId().equals(id) || suggestion.basePlanVersion() != plan.getVersion()) {
            throw new ConflictException("O plano mudou desde a análise; solicite uma nova sugestão");
        }
        if (!"AVAILABLE".equals(suggestion.status()) || suggestion.expiresAt().isBefore(java.time.Instant.now())) {
            throw new ConflictException("A sugestão expirou ou não está mais disponível");
        }
        if (applications.existsBySuggestionId(suggestion.id())) throw new ConflictException("Esta sugestão já foi aplicada");
        List<PlanModels.DayRequest> changed = applyChanges(requestDays(plan), suggestion.changes());
        validator.validateStructure(changed, false);
        validateEligibility(plan.getUnitId(), changed, token);
        plan.replace(plan.getUnitId(), plan.getName(), changed.stream().map(this::toEntity).toList());
        plan.confirmInventoryRevalidation();
        repository.saveAndFlush(plan);
        applications.saveAndFlush(new SuggestionApplication(subject, idempotencyKey, suggestion.id(), id, plan.getVersion()));
        return response(plan, true, List.of());
    }

    private List<PlanModels.DayRequest> applyChanges(List<PlanModels.DayRequest> original,
                                                      List<AssistantSuggestionClient.Change> changes) {
        List<PlanModels.DayRequest> result = new ArrayList<>(original);
        for (AssistantSuggestionClient.Change change : changes) {
            int dayIndex = -1;
            for (int index = 0; index < result.size(); index++) if (result.get(index).id().equals(change.dayId())) { dayIndex = index; break; }
            if (dayIndex < 0) throw new ConflictException("A sugestão referencia um dia inexistente");
            PlanModels.DayRequest day = result.get(dayIndex);
            List<PlanModels.ItemRequest> items = new ArrayList<>(day.items());
            int targetIndex = -1;
            if (change.targetItemId() != null) {
                for (int index = 0; index < items.size(); index++) if (items.get(index).id().equals(change.targetItemId())) { targetIndex = index; break; }
            }
            switch (change.operation()) {
                case ADD -> {
                    int insert = change.position() == null ? items.size() : change.position() - 1;
                    if (insert < 0 || insert > items.size() || change.exerciseId() == null) throw new ConflictException("Operação ADD inválida");
                    items.add(insert, suggestedItem(null, change, insert + 1));
                }
                case REPLACE -> {
                    if (targetIndex < 0 || change.exerciseId() == null) throw new ConflictException("Operação REPLACE inválida");
                    items.set(targetIndex, suggestedItem(items.get(targetIndex).id(), change, targetIndex + 1));
                }
                case REMOVE -> {
                    if (targetIndex < 0) throw new ConflictException("Operação REMOVE inválida");
                    items.remove(targetIndex);
                }
            }
            List<PlanModels.ItemRequest> normalized = new ArrayList<>();
            for (int index = 0; index < items.size(); index++) {
                PlanModels.ItemRequest item = items.get(index);
                normalized.add(new PlanModels.ItemRequest(item.id(), item.exerciseId(), index + 1, item.sets(),
                    item.repetitionMin(), item.repetitionMax(), item.durationSeconds(), item.restSeconds(),
                    item.optionalLoadKg(), item.notes()));
            }
            result.set(dayIndex, new PlanModels.DayRequest(day.id(), day.position(), day.name(), normalized));
        }
        return result;
    }

    private PlanModels.ItemRequest suggestedItem(UUID id, AssistantSuggestionClient.Change change, int position) {
        return new PlanModels.ItemRequest(id, change.exerciseId(), position, change.sets(), change.repetitionMin(),
            change.repetitionMax(), change.durationSeconds(), change.restSeconds(), null, null);
    }

    private void validateEligibility(UUID unitId, List<PlanModels.DayRequest> days, String token) {
        Set<UUID> exerciseIds = days.stream().flatMap(day -> day.items().stream()).map(PlanModels.ItemRequest::exerciseId)
            .collect(Collectors.toSet());
        if (exerciseIds.isEmpty()) return;
        GymCatalogClient.EligibilityResult result = catalog.validate(unitId, exerciseIds, token);
        List<UUID> invalid = invalidIds(result);
        if (!invalid.isEmpty()) throw new EligibilityConflictException(
            "Um ou mais exercícios não estão disponíveis na unidade selecionada", invalid);
        validator.validateKinds(days, result);
    }

    private List<UUID> invalidIds(GymCatalogClient.EligibilityResult result) {
        return result.results().stream().filter(item -> !item.eligible()).map(GymCatalogClient.EligibilityItem::exerciseId).toList();
    }

    private List<PlanModels.PlanIssue> inventoryIssues(WorkoutPlan plan, List<PlanModels.PlanIssue> issues) {
        if (!plan.isInventoryRevalidationRequired()) return issues;
        List<PlanModels.PlanIssue> result = new ArrayList<>(issues);
        result.add(new PlanModels.PlanIssue("INVENTORY_CHANGED",
            "O inventário da unidade mudou; revise a disponibilidade antes da próxima alteração", List.of()));
        return List.copyOf(result);
    }

    private WorkoutPlan owned(String subject, UUID id) {
        return repository.findOneByIdAndIdentitySubject(id, subject)
            .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado"));
    }

    private void requireVersion(WorkoutPlan plan, long expected) {
        if (plan.getVersion() != expected) throw new ConflictException("O plano foi alterado em outra sessão; recarregue antes de salvar");
    }

    private WorkoutDay toEntity(PlanModels.DayRequest day) {
        List<WorkoutItem> items = day.items().stream().map(item -> new WorkoutItem(item.id(), item.exerciseId(), item.position(),
            item.sets(), item.repetitionMin(), item.repetitionMax(), item.durationSeconds(), item.restSeconds(),
            item.optionalLoadKg(), item.notes())).toList();
        return new WorkoutDay(day.id(), day.position(), day.name(), items);
    }

    private List<PlanModels.DayRequest> requestDays(WorkoutPlan plan) {
        return plan.getDays().stream().map(day -> new PlanModels.DayRequest(day.getId(), day.getPosition(), day.getName(),
            day.getItems().stream().map(item -> new PlanModels.ItemRequest(item.getId(), item.getExerciseId(), item.getPosition(),
                item.getSets(), item.getRepetitionMin(), item.getRepetitionMax(), item.getDurationSeconds(), item.getRestSeconds(),
                item.getOptionalLoadKg(), item.getNotes())).toList())).toList();
    }

    private Set<UUID> exerciseIds(WorkoutPlan plan) {
        return plan.getDays().stream().flatMap(day -> day.getItems().stream()).map(WorkoutItem::getExerciseId).collect(Collectors.toSet());
    }

    private PlanModels.PlanResponse response(WorkoutPlan plan, boolean available, List<PlanModels.PlanIssue> issues) {
        List<PlanModels.DayResponse> days = plan.getDays().stream().map(day -> new PlanModels.DayResponse(day.getId(), day.getPosition(),
            day.getName(), day.getItems().stream().map(item -> new PlanModels.ItemResponse(item.getId(), item.getExerciseId(),
                item.getPosition(), item.getSets(), item.getRepetitionMin(), item.getRepetitionMax(), item.getDurationSeconds(),
                item.getRestSeconds(), item.getOptionalLoadKg(), item.getNotes())).toList())).toList();
        return new PlanModels.PlanResponse(plan.getId(), plan.getUnitId(), plan.getName(), plan.getStatus(), plan.getVersion(),
            days, plan.isInventoryRevalidationRequired(), available, issues, plan.getCreatedAt(), plan.getUpdatedAt());
    }
}
