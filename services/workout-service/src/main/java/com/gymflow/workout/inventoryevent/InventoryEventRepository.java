package com.gymflow.workout.inventoryevent;

import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryEventRepository {
    private final JdbcTemplate jdbc;
    public InventoryEventRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean recordProcessed(EquipmentAvailabilityChanged event) {
        return jdbc.update("""
            INSERT INTO processed_inventory_events(event_id, event_type, unit_id, equipment_type_id,
              aggregate_version, processed_at)
            VALUES (?,?,?,?,?,?) ON CONFLICT (event_id) DO NOTHING
            """, event.eventId(), event.eventType(), event.unitId(), event.equipmentTypeId(), event.aggregateVersion(),
            Timestamp.from(event.occurredAt())) == 1;
    }

    public boolean acceptNewerVersion(EquipmentAvailabilityChanged event) {
        return jdbc.update("""
            INSERT INTO inventory_event_versions(unit_id, equipment_type_id, aggregate_version, updated_at)
            VALUES (?,?,?,now())
            ON CONFLICT (unit_id, equipment_type_id) DO UPDATE
              SET aggregate_version=EXCLUDED.aggregate_version, updated_at=now()
            WHERE inventory_event_versions.aggregate_version < EXCLUDED.aggregate_version
            """, event.unitId(), event.equipmentTypeId(), event.aggregateVersion()) == 1;
    }

    public int flagPotentiallyAffectedPlans(EquipmentAvailabilityChanged event) {
        return jdbc.update("""
            UPDATE workout_plans
            SET inventory_revalidation_required=TRUE
            WHERE unit_id=? AND status <> 'ARCHIVED'
            """, event.unitId());
    }
}
