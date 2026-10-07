package com.gymflow.workout.plan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, UUID> {
    List<WorkoutPlan> findByIdentitySubjectOrderByUpdatedAtDesc(String identitySubject);
    Optional<WorkoutPlan> findOneByIdAndIdentitySubject(UUID id, String identitySubject);
}
