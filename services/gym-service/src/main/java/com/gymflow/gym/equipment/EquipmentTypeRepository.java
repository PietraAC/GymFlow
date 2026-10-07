package com.gymflow.gym.equipment;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentTypeRepository extends JpaRepository<EquipmentType, UUID> {
    Page<EquipmentType> findByActiveTrue(Pageable pageable);
}

