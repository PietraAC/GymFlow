package com.gymflow.workout.plan;

import com.gymflow.workout.integration.gym.GymCatalogClient;
import com.gymflow.workout.shared.error.ConflictException;
import com.gymflow.workout.shared.error.DependencyUnavailableException;
import com.gymflow.workout.shared.error.EligibilityConflictException;
import com.gymflow.workout.shared.error.ResourceNotFoundException;
import java.util.List;
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

    public WorkoutPlanService(WorkoutPlanRepository repository, GymCatalogClient catalog, WorkoutPlanValidator validator) {
        this.repository = repository; this.catalog = catalog; this.validator = validator;
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
                plan.getVersion(), plan.getDays().size(), plan.getUpdatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public PlanModels.PlanResponse get(String subject, UUID id, String token) {
        WorkoutPlan plan = owned(subject, id);
        Set<UUID> ids = exerciseIds(plan);
        if (ids.isEmpty()) return response(plan, true, List.of());
        try {
            GymCatalogClient.EligibilityResult result = catalog.validate(plan.getUnitId(), ids, token);
            List<UUID> invalid = invalidIds(result);
            List<PlanModels.PlanIssue> issues = invalid.isEmpty() ? List.of() : List.of(new PlanModels.PlanIssue(
                "EXERCISE_INELIGIBLE", "Há exercícios indisponíveis nesta unidade; ajuste o rascunho antes de ativar", invalid));
            return response(plan, true, issues);
        } catch (DependencyUnavailableException exception) {
            return response(plan, false, List.of(new PlanModels.PlanIssue("ELIGIBILITY_UNAVAILABLE",
                "A elegibilidade atual não pôde ser consultada", List.of())));
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
            days, available, issues, plan.getCreatedAt(), plan.getUpdatedAt());
    }
}
