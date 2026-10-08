package com.gymflow.assistant.integration;

import com.gymflow.assistant.conversation.AssistantModels;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AssistantContext(Profile profile, Plan plan, List<Exercise> eligibleExercises,
                               List<AssistantModels.MessageResponse> history, String fingerprint) {
    public record Profile(String goal, String experienceLevel, int daysPerWeek, int sessionDurationMinutes,
                          Set<UUID> preferredEquipmentTypeIds) {}
    public record Plan(UUID id, UUID unitId, String name, String status, long version, List<Day> days) {}
    public record Day(UUID id, int position, String name, List<Item> items) {}
    public record Item(UUID id, UUID exerciseId, int position, Integer sets, Integer repetitionMin,
                       Integer repetitionMax, Integer durationSeconds, Integer restSeconds, String notes) {}
    public record Exercise(UUID id, String name, String kind, Set<String> primaryMuscleGroups,
                           Set<String> secondaryMuscleGroups, String movementPattern, String difficulty,
                           String instructions, List<RequirementOption> requirementOptions) {}
    public record RequirementOption(UUID id, int position, Set<UUID> equipmentTypeIds) {}
    public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}
