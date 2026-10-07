package com.gymflow.gym.equipment;

import com.gymflow.gym.shared.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "unit_equipment")
public class UnitEquipment extends AuditedEntity {
    @Id
    private UUID id;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "equipment_type_id", nullable = false)
    private UUID equipmentTypeId;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(length = 500)
    private String notes;

    @Version
    @Column(nullable = false)
    private long version;

    protected UnitEquipment() {}

    public UnitEquipment(UUID unitId, UUID equipmentTypeId, int totalQuantity, int availableQuantity, String notes) {
        this.id = UUID.randomUUID();
        this.unitId = unitId;
        this.equipmentTypeId = equipmentTypeId;
        update(totalQuantity, availableQuantity, notes);
    }

    public void update(int totalQuantity, int availableQuantity, String notes) {
        if (totalQuantity < 0 || availableQuantity < 0 || availableQuantity > totalQuantity) {
            throw new IllegalArgumentException("A quantidade disponível deve estar entre zero e a quantidade total");
        }
        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
        this.notes = notes;
    }

    public UUID getId() { return id; }
    public UUID getUnitId() { return unitId; }
    public UUID getEquipmentTypeId() { return equipmentTypeId; }
    public int getTotalQuantity() { return totalQuantity; }
    public int getAvailableQuantity() { return availableQuantity; }
    public String getNotes() { return notes; }
    public long getVersion() { return version; }
}

