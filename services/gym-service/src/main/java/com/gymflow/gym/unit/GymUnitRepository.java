package com.gymflow.gym.unit;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymUnitRepository extends JpaRepository<GymUnit, UUID> {
    Page<GymUnit> findByGymIdAndActiveTrue(UUID gymId, Pageable pageable);
    Page<GymUnit> findByGymId(UUID gymId, Pageable pageable);
}

