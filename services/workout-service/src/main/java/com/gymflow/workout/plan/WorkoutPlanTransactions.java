package com.gymflow.workout.plan;

import com.gymflow.workout.suggestion.SuggestionApplication;
import com.gymflow.workout.suggestion.SuggestionApplicationRepository;
import com.gymflow.workout.shared.error.ConflictException;
import com.gymflow.workout.shared.error.InvalidRequestException;
import com.gymflow.workout.shared.error.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkoutPlanTransactions {
    private final WorkoutPlanRepository plans;
    private final SuggestionApplicationRepository applications;
    private final WorkoutPlanMapper mapper;

    public WorkoutPlanTransactions(WorkoutPlanRepository plans, SuggestionApplicationRepository applications,
                                   WorkoutPlanMapper mapper) {
        this.plans = plans;
        this.applications = applications;
        this.mapper = mapper;
    }

    @Transactional
    public PlanModels.PlanResponse create(String subject, PlanModels.CreatePlanRequest request) {
        WorkoutPlan plan = plans.save(new WorkoutPlan(subject, request.unitId(), request.name(), request.goal(),
            request.targetDaysPerWeek()));
        return mapper.response(plan, true, List.of());
    }

    @Transactional(readOnly = true)
    public List<PlanModels.PlanSummary> list(String subject) {
        return plans.findByIdentitySubjectOrderByUpdatedAtDesc(subject).stream().map(mapper::summary).toList();
    }

    @Transactional(readOnly = true)
    public PlanModels.PlanResponse owned(String subject, UUID id) {
        return mapper.response(ownedEntity(subject, id), true, List.of());
    }

    @Transactional
    public PlanModels.PlanResponse update(String subject, UUID id, PlanModels.UpdatePlanRequest request,
                                          List<WorkoutDay> days) {
        WorkoutPlan plan = locked(subject, id);
        requireVersion(plan, request.version());
        requireStatus(plan, PlanStatus.DRAFT, "Somente planos em rascunho podem ser editados");
        requireSameUnit(plan, request.unitId());
        plan.replace(request.unitId(), request.name(), request.goal(), request.targetDaysPerWeek(), days);
        plan.confirmInventoryRevalidation();
        plans.saveAndFlush(plan);
        return mapper.response(plan, true, List.of());
    }

    @Transactional
    public PlanModels.PlanResponse activate(String subject, UUID id, long version) {
        WorkoutPlan plan = locked(subject, id);
        requireVersion(plan, version);
        if (plan.getStatus() == PlanStatus.ARCHIVED) throw new ConflictException("Um plano arquivado não pode ser ativado");
        plan.confirmInventoryRevalidation();
        plan.activate();
        plans.saveAndFlush(plan);
        return mapper.response(plan, true, List.of());
    }

    @Transactional
    public PlanModels.PlanResponse revalidate(String subject, UUID id, long version) {
        WorkoutPlan plan = locked(subject, id);
        requireVersion(plan, version);
        requireStatus(plan, PlanStatus.ACTIVE, "Somente planos ativos podem ser revalidados");
        plan.confirmInventoryRevalidation();
        plans.saveAndFlush(plan);
        return mapper.response(plan, true, List.of());
    }

    @Transactional
    public PlanModels.PlanResponse archive(String subject, UUID id, long version) {
        WorkoutPlan plan = locked(subject, id);
        requireVersion(plan, version);
        plan.archive();
        plans.saveAndFlush(plan);
        return mapper.response(plan, true, List.of());
    }

    @Transactional(readOnly = true)
    public Optional<PlanModels.PlanResponse> previousApplication(String subject, String key, UUID planId,
                                                                  UUID suggestionId) {
        return applications.findByIdentitySubjectAndIdempotencyKey(subject, key).map(application -> {
            if (!application.getPlanId().equals(planId) || !application.getSuggestionId().equals(suggestionId)) {
                throw new ConflictException("A Idempotency-Key já foi usada para outra operação");
            }
            return mapper.response(ownedEntity(subject, planId), true, List.of());
        });
    }

    @Transactional
    public PlanModels.PlanResponse applySuggestion(String subject, UUID id, long expectedVersion,
                                                    String idempotencyKey, UUID suggestionId,
                                                    List<WorkoutDay> changedDays) {
        WorkoutPlan plan = locked(subject, id);
        Optional<SuggestionApplication> previous = applications.findByIdentitySubjectAndIdempotencyKey(subject,
            idempotencyKey);
        if (previous.isPresent()) {
            SuggestionApplication application = previous.get();
            if (!application.getPlanId().equals(id) || !application.getSuggestionId().equals(suggestionId)) {
                throw new ConflictException("A Idempotency-Key já foi usada para outra operação");
            }
            return mapper.response(plan, true, List.of());
        }
        requireVersion(plan, expectedVersion);
        requireStatus(plan, PlanStatus.DRAFT, "Somente planos em rascunho aceitam sugestões");
        if (applications.existsBySuggestionId(suggestionId)) throw new ConflictException("Esta sugestão já foi aplicada");
        plan.replace(plan.getUnitId(), plan.getName(), plan.getGoal(), plan.getTargetDaysPerWeek(), changedDays);
        plan.confirmInventoryRevalidation();
        plans.saveAndFlush(plan);
        applications.saveAndFlush(new SuggestionApplication(subject, idempotencyKey, suggestionId, id,
            plan.getVersion()));
        return mapper.response(plan, true, List.of());
    }

    private WorkoutPlan ownedEntity(String subject, UUID id) {
        return plans.findOneByIdAndIdentitySubject(id, subject)
            .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado"));
    }

    private WorkoutPlan locked(String subject, UUID id) {
        return plans.findOwnedForUpdate(id, subject)
            .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado"));
    }

    private void requireVersion(WorkoutPlan plan, long expected) {
        if (plan.getVersion() != expected) {
            throw new ConflictException("O plano foi alterado em outra sessão; recarregue antes de salvar");
        }
    }

    private void requireStatus(WorkoutPlan plan, PlanStatus expected, String message) {
        if (plan.getStatus() != expected) throw new ConflictException(message);
    }

    private void requireSameUnit(WorkoutPlan plan, UUID unitId) {
        if (!plan.getUnitId().equals(unitId)) {
            throw new InvalidRequestException(
                "A unidade do plano é imutável; crie outro plano para treinar em uma unidade diferente");
        }
    }
}
