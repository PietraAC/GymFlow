package com.gymflow.gym.gym;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymRepository extends JpaRepository<Gym, UUID> {
    Page<Gym> findByActiveTrue(Pageable pageable);
    Page<Gym> findByIdIn(Collection<UUID> ids, Pageable pageable);
}

