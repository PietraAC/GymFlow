package com.gymflow.gym.equipment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitEquipmentRepository extends JpaRepository<UnitEquipment, UUID> {
    List<UnitEquipment> findByUnitIdOrderByEquipmentTypeId(UUID unitId);
    Optional<UnitEquipment> findByUnitIdAndEquipmentTypeId(UUID unitId, UUID equipmentTypeId);
}

