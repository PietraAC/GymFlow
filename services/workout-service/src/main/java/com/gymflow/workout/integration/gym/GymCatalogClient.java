package com.gymflow.workout.integration.gym;

import com.gymflow.workout.plan.ExerciseKind;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface GymCatalogClient {
    void validateUnit(UUID unitId, String bearerToken);
    EligibilityResult validate(UUID unitId, Set<UUID> exerciseIds, String bearerToken);
    record EligibilityResult(UUID unitId, List<EligibilityItem> results) {}
    record EligibilityItem(UUID exerciseId, ExerciseKind kind, boolean eligible, Set<String> reasons) {}
}
