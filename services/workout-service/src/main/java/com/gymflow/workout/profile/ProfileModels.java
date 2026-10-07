package com.gymflow.workout.profile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class ProfileModels {
    private ProfileModels() {}
    public record ProfileRequest(@NotNull Goal goal, @NotNull ExperienceLevel experienceLevel,
        @Min(1) @Max(7) int daysPerWeek, @Min(10) @Max(180) int sessionDurationMinutes,
        @Size(max = 100) Set<UUID> preferredEquipmentTypeIds) {}
    public record ProfileResponse(String identitySubject, Goal goal, ExperienceLevel experienceLevel, int daysPerWeek,
        int sessionDurationMinutes, Set<UUID> preferredEquipmentTypeIds, Instant createdAt, Instant updatedAt) {
        static ProfileResponse from(StudentProfile profile) {
            return new ProfileResponse(profile.getIdentitySubject(), profile.getGoal(), profile.getExperienceLevel(),
                profile.getDaysPerWeek(), profile.getSessionDurationMinutes(), profile.getPreferredEquipmentTypeIds(),
                profile.getCreatedAt(), profile.getUpdatedAt());
        }
    }
}
