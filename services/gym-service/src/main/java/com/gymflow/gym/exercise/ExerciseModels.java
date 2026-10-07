package com.gymflow.gym.exercise;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ExerciseModels {
    private ExerciseModels() {}

    public record RequirementOptionResponse(UUID id, int position, Set<UUID> equipmentTypeIds) {}

    public record ExerciseResponse(UUID id, String name, ExerciseKind kind, Set<String> primaryMuscleGroups,
                                   Set<String> secondaryMuscleGroups, String movementPattern, Difficulty difficulty,
                                   String instructions, List<RequirementOptionResponse> requirementOptions) {
        public static ExerciseResponse from(Exercise exercise) {
            List<RequirementOptionResponse> options = exercise.getRequirementOptions().stream()
                .map(option -> new RequirementOptionResponse(option.getId(), option.getPosition(),
                    option.getRequiredEquipmentTypes().stream().map(type -> type.getId()).collect(java.util.stream.Collectors.toSet())))
                .toList();
            return new ExerciseResponse(exercise.getId(), exercise.getName(), exercise.getKind(),
                exercise.getPrimaryMuscleGroups(), exercise.getSecondaryMuscleGroups(), exercise.getMovementPattern(),
                exercise.getDifficulty(), exercise.getInstructions(), options);
        }
    }
}

