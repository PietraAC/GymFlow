package com.gymflow.gym.eligibility;

import com.gymflow.gym.exercise.ExerciseModels;
import com.gymflow.gym.exercise.ExerciseKind;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class EligibilityModels {
    private EligibilityModels() {}
    public record EligibilityRequest(@NotEmpty @Size(max = 100) Set<UUID> exerciseIds) {}
    public record EligibilityItem(UUID exerciseId, ExerciseKind kind, boolean eligible, Set<EligibilityReason> reasons) {
        public static EligibilityItem from(EligibilityDecision decision, ExerciseKind kind) {
            return new EligibilityItem(decision.exerciseId(), kind, decision.eligible(), decision.reasons());
        }
    }
    public record EligibilityResponse(UUID unitId, List<EligibilityItem> results) {}
    public record EligibleExerciseResponse(ExerciseModels.ExerciseResponse exercise) {}
}
