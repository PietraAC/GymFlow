package com.gymflow.gym.exercise;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID>, JpaSpecificationExecutor<Exercise> {
    @EntityGraph(attributePaths = {"primaryMuscleGroups", "secondaryMuscleGroups", "requirementOptions", "requirementOptions.requiredEquipmentTypes"})
    List<Exercise> findByActiveTrueOrderByNameAsc();

    @EntityGraph(attributePaths = {"primaryMuscleGroups", "secondaryMuscleGroups", "requirementOptions", "requirementOptions.requiredEquipmentTypes"})
    List<Exercise> findByIdIn(Collection<UUID> ids);
}

