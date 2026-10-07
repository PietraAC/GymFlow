package com.gymflow.workout.shared.error;

import java.util.List;
import java.util.UUID;

public class EligibilityConflictException extends ConflictException {
    private final List<UUID> exerciseIds;
    public EligibilityConflictException(String message, List<UUID> exerciseIds) { super(message); this.exerciseIds = List.copyOf(exerciseIds); }
    public List<UUID> getExerciseIds() { return exerciseIds; }
}
