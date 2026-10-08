package com.gymflow.gym.inventoryevent;

import java.time.Instant;
import java.util.UUID;

public record EquipmentAvailabilityChanged(UUID eventId, String eventType, int schemaVersion, Instant occurredAt,
                                           UUID gymId, UUID unitId, UUID equipmentTypeId,
                                           int availableQuantity, long aggregateVersion) {
    public static final String TYPE = "EquipmentAvailabilityChanged.v1";

    public static EquipmentAvailabilityChanged create(UUID gymId, UUID unitId, UUID equipmentTypeId,
                                                       int availableQuantity, long aggregateVersion) {
        return new EquipmentAvailabilityChanged(UUID.randomUUID(), TYPE, 1, Instant.now(), gymId, unitId,
            equipmentTypeId, availableQuantity, aggregateVersion);
    }
}
