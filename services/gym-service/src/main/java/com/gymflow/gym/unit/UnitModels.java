package com.gymflow.gym.unit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class UnitModels {
    private UnitModels() {}

    public record CreateUnitRequest(@NotBlank @Size(max = 120) String name,
                                    @NotBlank @Size(max = 100) String city,
                                    Boolean active) {}
    public record UpdateUnitRequest(@Size(min = 1, max = 120) String name,
                                    @Size(min = 1, max = 100) String city,
                                    Boolean active) {}
    public record UnitResponse(UUID id, UUID gymId, String name, String city, boolean active,
                               Instant createdAt, Instant updatedAt) {
        public static UnitResponse from(GymUnit unit) {
            return new UnitResponse(unit.getId(), unit.getGymId(), unit.getName(), unit.getCity(), unit.isActive(),
                unit.getCreatedAt(), unit.getUpdatedAt());
        }
    }
}

