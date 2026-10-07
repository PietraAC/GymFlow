package com.gymflow.gym.equipment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class EquipmentModels {
    private EquipmentModels() {}

    public record EquipmentTypeResponse(UUID id, String name, String description, boolean active) {
        public static EquipmentTypeResponse from(EquipmentType type) {
            return new EquipmentTypeResponse(type.getId(), type.getName(), type.getDescription(), type.isActive());
        }
    }

    public record UpsertUnitEquipmentRequest(@Min(0) @Max(10000) int totalQuantity,
                                             @Min(0) @Max(10000) int availableQuantity,
                                             @Size(max = 500) String notes,
                                             Long version) {}

    public record UnitEquipmentResponse(UUID id, UUID unitId, UUID equipmentTypeId, String equipmentTypeName,
                                        int totalQuantity, int availableQuantity, String notes, long version,
                                        Instant createdAt, Instant updatedAt) {
        public static UnitEquipmentResponse from(UnitEquipment item, EquipmentType type) {
            return new UnitEquipmentResponse(item.getId(), item.getUnitId(), item.getEquipmentTypeId(), type.getName(),
                item.getTotalQuantity(), item.getAvailableQuantity(), item.getNotes(), item.getVersion(),
                item.getCreatedAt(), item.getUpdatedAt());
        }
    }
}

