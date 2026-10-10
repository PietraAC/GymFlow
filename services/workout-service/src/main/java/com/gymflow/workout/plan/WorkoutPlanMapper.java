package com.gymflow.workout.plan;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WorkoutPlanMapper {
    public PlanModels.PlanSummary summary(WorkoutPlan plan) {
        return new PlanModels.PlanSummary(plan.getId(), plan.getUnitId(), plan.getName(), plan.getGoal(),
            plan.getTargetDaysPerWeek(), plan.getStatus(), plan.getVersion(), plan.getDays().size(),
            plan.isInventoryRevalidationRequired(), plan.getUpdatedAt());
    }

    public PlanModels.PlanResponse response(WorkoutPlan plan, boolean eligibilityAvailable,
                                            List<PlanModels.PlanIssue> issues) {
        List<PlanModels.DayResponse> days = plan.getDays().stream().map(day -> new PlanModels.DayResponse(
            day.getId(), day.getPosition(), day.getName(), day.getItems().stream().map(item ->
                new PlanModels.ItemResponse(item.getId(), item.getExerciseId(), item.getPosition(), item.getSets(),
                    item.getRepetitionMin(), item.getRepetitionMax(), item.getDurationSeconds(), item.getRestSeconds(),
                    item.getOptionalLoadKg(), item.getNotes())).toList())).toList();
        return new PlanModels.PlanResponse(plan.getId(), plan.getUnitId(), plan.getName(), plan.getGoal(),
            plan.getTargetDaysPerWeek(), plan.getStatus(), plan.getVersion(), days,
            plan.isInventoryRevalidationRequired(), eligibilityAvailable, issues, plan.getCreatedAt(), plan.getUpdatedAt());
    }

    public List<PlanModels.DayRequest> requests(WorkoutPlan plan) {
        return plan.getDays().stream().map(day -> new PlanModels.DayRequest(day.getId(), day.getPosition(), day.getName(),
            day.getItems().stream().map(item -> new PlanModels.ItemRequest(item.getId(), item.getExerciseId(),
                item.getPosition(), item.getSets(), item.getRepetitionMin(), item.getRepetitionMax(),
                item.getDurationSeconds(), item.getRestSeconds(), item.getOptionalLoadKg(), item.getNotes())).toList()))
            .toList();
    }

    public List<WorkoutDay> entities(List<PlanModels.DayRequest> days) {
        return days.stream().map(day -> new WorkoutDay(day.id(), day.position(), day.name(), day.items().stream()
            .map(item -> new WorkoutItem(item.id(), item.exerciseId(), item.position(), item.sets(),
                item.repetitionMin(), item.repetitionMax(), item.durationSeconds(), item.restSeconds(),
                item.optionalLoadKg(), item.notes())).toList())).toList();
    }
}
