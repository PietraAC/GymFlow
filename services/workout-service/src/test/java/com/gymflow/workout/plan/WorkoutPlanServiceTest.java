package com.gymflow.workout.plan;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymflow.workout.integration.gym.GymCatalogClient;
import com.gymflow.workout.shared.error.ConflictException;
import com.gymflow.workout.shared.error.DependencyUnavailableException;
import com.gymflow.workout.shared.error.EligibilityConflictException;
import java.util.List;
import com.gymflow.workout.shared.error.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class WorkoutPlanServiceTest {
    @Mock WorkoutPlanRepository repository;
    @Mock GymCatalogClient catalog;
    private WorkoutPlanService service;

    @BeforeEach void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new WorkoutPlanService(repository, catalog, new WorkoutPlanValidator());
    }

    @Test
    void neverReturnsAPlanOwnedByAnotherSubject() {
        UUID id = UUID.randomUUID();
        when(repository.findOneByIdAndIdentitySubject(id, "student-b")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get("student-b", id, "token"))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(catalog, never()).validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void staleVersionIsRejectedBeforeMutation() {
        UUID id = UUID.randomUUID();
        WorkoutPlan plan = new WorkoutPlan("student-a", UUID.randomUUID(), "Plano");
        when(repository.findOneByIdAndIdentitySubject(id, "student-a")).thenReturn(Optional.of(plan));
        assertThatThrownBy(() -> service.archive("student-a", id, 1))
            .isInstanceOf(ConflictException.class).hasMessageContaining("outra sessão");
        verify(repository, never()).saveAndFlush(plan);
    }

    @Test
    void updateRevalidatesInventoryAndRejectsIneligibleExercise() {
        UUID id = UUID.randomUUID(), unitId = UUID.randomUUID(), exerciseId = UUID.randomUUID();
        WorkoutPlan plan = new WorkoutPlan("student-a", unitId, "Plano");
        when(repository.findOneByIdAndIdentitySubject(id, "student-a")).thenReturn(Optional.of(plan));
        when(catalog.validate(org.mockito.ArgumentMatchers.eq(unitId), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("token")))
            .thenReturn(new GymCatalogClient.EligibilityResult(unitId,
                List.of(new GymCatalogClient.EligibilityItem(exerciseId, ExerciseKind.STRENGTH, false, java.util.Set.of("EQUIPMENT_UNAVAILABLE")))));

        assertThatThrownBy(() -> service.update("student-a", id, request(unitId, exerciseId), "token"))
            .isInstanceOf(EligibilityConflictException.class);
        verify(repository, never()).saveAndFlush(plan);
    }

    @Test
    void dependencyFailureClosesMutation() {
        UUID id = UUID.randomUUID(), unitId = UUID.randomUUID(), exerciseId = UUID.randomUUID();
        WorkoutPlan plan = new WorkoutPlan("student-a", unitId, "Plano");
        when(repository.findOneByIdAndIdentitySubject(id, "student-a")).thenReturn(Optional.of(plan));
        when(catalog.validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenThrow(new DependencyUnavailableException("gym-service indisponível", new RuntimeException()));

        assertThatThrownBy(() -> service.update("student-a", id, request(unitId, exerciseId), "token"))
            .isInstanceOf(DependencyUnavailableException.class);
        verify(repository, never()).saveAndFlush(plan);
    }

    private PlanModels.UpdatePlanRequest request(UUID unitId, UUID exerciseId) {
        PlanModels.ItemRequest item = new PlanModels.ItemRequest(null, exerciseId, 1, 3, 8, 12, null, 60, null, null);
        return new PlanModels.UpdatePlanRequest(0L, unitId, "Plano", List.of(new PlanModels.DayRequest(null, 1, "Dia 1", List.of(item))));
    }
}
