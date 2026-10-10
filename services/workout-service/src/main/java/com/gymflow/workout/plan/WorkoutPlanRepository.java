package com.gymflow.workout.plan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, UUID> {
    List<WorkoutPlan> findByIdentitySubjectOrderByUpdatedAtDesc(String identitySubject);
    Optional<WorkoutPlan> findOneByIdAndIdentitySubject(UUID id, String identitySubject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from WorkoutPlan plan where plan.id = :id and plan.identitySubject = :subject")
    Optional<WorkoutPlan> findOwnedForUpdate(@Param("id") UUID id, @Param("subject") String subject);
}
