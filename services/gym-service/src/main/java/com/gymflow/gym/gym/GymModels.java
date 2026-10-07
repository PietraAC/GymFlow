package com.gymflow.gym.gym;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class GymModels {
    private GymModels() {}

    public record CreateGymRequest(@NotBlank @Size(max = 120) String name, Boolean active) {}

    public record GymResponse(UUID id, String name, boolean active, Instant createdAt, Instant updatedAt) {
        public static GymResponse from(Gym gym) {
            return new GymResponse(gym.getId(), gym.getName(), gym.isActive(), gym.getCreatedAt(), gym.getUpdatedAt());
        }
    }
}

