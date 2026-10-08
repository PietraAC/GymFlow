package com.gymflow.workout.plan;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import com.gymflow.workout.integration.gym.GymCatalogClient;
import com.gymflow.workout.shared.error.ConflictException;
import com.gymflow.workout.shared.error.DependencyUnavailableException;
import com.gymflow.workout.shared.error.EligibilityConflictException;
import java.util.List;
import com.gymflow.workout.shared.error.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.gymflow.workout.integration.assistant.AssistantSuggestionClient;
import com.gymflow.workout.suggestion.SuggestionApplicationRepository;
import com.gymflow.workout.profile.Goal;

class WorkoutPlanServiceTest {
    @Mock WorkoutPlanRepository repository;
    @Mock GymCatalogClient catalog;
    @Mock AssistantSuggestionClient suggestions;
    @Mock SuggestionApplicationRepository applications;
    private WorkoutPlanService service;

    @BeforeEach void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new WorkoutPlanService(repository, catalog, new WorkoutPlanValidator(), suggestions, applications);
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

    @Test
    void appliesAValidatedSuggestionAndRecordsIdempotency() {
        UUID planId = UUID.randomUUID(), unitId = UUID.randomUUID(), dayId = UUID.randomUUID();
        UUID currentExercise = UUID.randomUUID(), suggestedExercise = UUID.randomUUID(), suggestionId = UUID.randomUUID();
        WorkoutPlan plan = new WorkoutPlan("student-a", unitId, "Plano");
        plan.replace(unitId, "Plano", List.of(new WorkoutDay(dayId, 1, "Dia 1",
            List.of(new WorkoutItem(null, currentExercise, 1, 3, 8, 12, null, 60, null, null)))));
        when(repository.findOneByIdAndIdentitySubject(planId, "student-a")).thenReturn(Optional.of(plan));
        when(applications.findByIdentitySubjectAndIdempotencyKey("student-a", "key-1")).thenReturn(Optional.empty());
        var change = new AssistantSuggestionClient.Change(AssistantSuggestionClient.Operation.ADD, dayId, null,
            suggestedExercise, 2, 3, 8, 12, null, 60, "Complemento");
        when(suggestions.get(suggestionId, "token")).thenReturn(new AssistantSuggestionClient.Suggestion(suggestionId,
            planId, 0, "fp", "AVAILABLE", "DEMO", "Explicação", List.of(), List.of(change), Instant.now(), Instant.now().plusSeconds(600)));
        when(catalog.validate(org.mockito.ArgumentMatchers.eq(unitId), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("token")))
            .thenReturn(new GymCatalogClient.EligibilityResult(unitId, List.of(
                new GymCatalogClient.EligibilityItem(currentExercise, ExerciseKind.STRENGTH, true, java.util.Set.of()),
                new GymCatalogClient.EligibilityItem(suggestedExercise, ExerciseKind.STRENGTH, true, java.util.Set.of()))));

        PlanModels.PlanResponse response = service.applySuggestion("student-a", planId,
            new PlanModels.ApplySuggestionRequest(suggestionId, 0L), "key-1", "token");

        assertThat(response.days().getFirst().items()).hasSize(2);
        verify(applications).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void appliesACompleteWorkoutSuggestionAndPreservesExistingContent() {
        UUID planId = UUID.randomUUID(), unitId = UUID.randomUUID(), dayId = UUID.randomUUID();
        UUID currentExercise = UUID.randomUUID(), addedExercise = UUID.randomUUID(), suggestionId = UUID.randomUUID();
        WorkoutPlan plan = new WorkoutPlan("student-a", unitId, "Plano", Goal.STRENGTH, 3);
        plan.replace(unitId, "Plano", Goal.STRENGTH, 3, List.of(new WorkoutDay(dayId, 1, "Dia 1",
            List.of(new WorkoutItem(null, currentExercise, 1, 3, 8, 12, null, 60, null, null)))));
        var itemAtTwo = new AssistantSuggestionClient.SuggestedItem(addedExercise, 2, 3, 8, 12, null, 60, "Complemento");
        var itemAtOne = new AssistantSuggestionClient.SuggestedItem(addedExercise, 1, 3, 8, 12, null, 60, "Complemento");
        var completion = new AssistantSuggestionClient.Completion(
            List.of(new AssistantSuggestionClient.SuggestedDay(2, "Dia 2", List.of(itemAtOne)),
                new AssistantSuggestionClient.SuggestedDay(3, "Dia 3", List.of(itemAtOne))),
            List.of(new AssistantSuggestionClient.ExistingDayAddition(dayId, List.of(itemAtTwo))));
        when(repository.findOneByIdAndIdentitySubject(planId, "student-a")).thenReturn(Optional.of(plan));
        when(applications.findByIdentitySubjectAndIdempotencyKey("student-a", "key-complete")).thenReturn(Optional.empty());
        when(suggestions.get(suggestionId, "token")).thenReturn(new AssistantSuggestionClient.Suggestion(suggestionId,
            planId, 0, "fp", "AVAILABLE", "DEMO", AssistantSuggestionClient.SuggestionKind.WORKOUT_COMPLETION,
            "Treino completo", List.of(), List.of(), completion, Instant.now(), Instant.now().plusSeconds(600)));
        when(catalog.validate(org.mockito.ArgumentMatchers.eq(unitId), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("token")))
            .thenReturn(new GymCatalogClient.EligibilityResult(unitId, List.of(
                new GymCatalogClient.EligibilityItem(currentExercise, ExerciseKind.STRENGTH, true, java.util.Set.of()),
                new GymCatalogClient.EligibilityItem(addedExercise, ExerciseKind.STRENGTH, true, java.util.Set.of()))));

        PlanModels.PlanResponse response = service.applySuggestion("student-a", planId,
            new PlanModels.ApplySuggestionRequest(suggestionId, 0L), "key-complete", "token");

        assertThat(response.days()).hasSize(3);
        assertThat(response.days().getFirst().items()).extracting(PlanModels.ItemResponse::exerciseId)
            .containsExactly(currentExercise, addedExercise);
        assertThat(response.goal()).isEqualTo(Goal.STRENGTH);
        assertThat(response.targetDaysPerWeek()).isEqualTo(3);
    }

    private PlanModels.UpdatePlanRequest request(UUID unitId, UUID exerciseId) {
        PlanModels.ItemRequest item = new PlanModels.ItemRequest(null, exerciseId, 1, 3, 8, 12, null, 60, null, null);
        return new PlanModels.UpdatePlanRequest(0L, unitId, "Plano", List.of(new PlanModels.DayRequest(null, 1, "Dia 1", List.of(item))));
    }
}
