package com.gymflow.workout.inventoryevent;

import java.time.Instant;
import java.util.UUID;

public record EquipmentAvailabilityChanged(UUID eventId, String eventType, int schemaVersion, Instant occurredAt,
                                           UUID gymId, UUID unitId, UUID equipmentTypeId,
                                           int availableQuantity, long aggregateVersion) {
    public static final String TYPE = "EquipmentAvailabilityChanged.v1";

    public void validate() {
        if (eventId == null || occurredAt == null || gymId == null || unitId == null || equipmentTypeId == null
            || !TYPE.equals(eventType) || schemaVersion != 1 || availableQuantity < 0 || aggregateVersion < 0) {
            throw new IllegalArgumentException("Evento de inventario invalido");
        }
    }
}
