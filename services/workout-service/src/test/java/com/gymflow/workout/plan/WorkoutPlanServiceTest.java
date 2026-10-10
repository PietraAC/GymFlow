package com.gymflow.workout.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymflow.workout.integration.assistant.AssistantSuggestionClient;
import com.gymflow.workout.integration.gym.GymCatalogClient;
import com.gymflow.workout.profile.Goal;
import com.gymflow.workout.shared.error.ConflictException;
import com.gymflow.workout.shared.error.DependencyUnavailableException;
import com.gymflow.workout.shared.error.EligibilityConflictException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class WorkoutPlanServiceTest {
    @Mock WorkoutPlanTransactions transactions;
    @Mock GymCatalogClient catalog;
    @Mock AssistantSuggestionClient suggestions;
    private WorkoutPlanService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new WorkoutPlanService(transactions, catalog, new WorkoutPlanValidator(), suggestions,
            new WorkoutPlanMapper(), new SuggestionApplier());
    }

    @Test
    void staleVersionIsRejectedBeforeRemoteValidation() {
        UUID planId = UUID.randomUUID(), unitId = UUID.randomUUID(), exerciseId = UUID.randomUUID();
        when(transactions.owned("student-a", planId)).thenReturn(plan(planId, unitId, 0,
            List.of(day(exerciseId))));

        assertThatThrownBy(() -> service.update("student-a", planId,
            new PlanModels.UpdatePlanRequest(1L, unitId, "Plano", List.of(requestDay(exerciseId))), "token"))
            .isInstanceOf(ConflictException.class).hasMessageContaining("outra sessão");
        verify(catalog, never()).validate(any(), any(), any());
    }

    @Test
    void updateRevalidatesInventoryAndRejectsIneligibleExercise() {
        UUID planId = UUID.randomUUID(), unitId = UUID.randomUUID(), exerciseId = UUID.randomUUID();
        when(transactions.owned("student-a", planId)).thenReturn(plan(planId, unitId, 0,
            List.of(day(exerciseId))));
        when(catalog.validate(eq(unitId), any(), eq("token"))).thenReturn(new GymCatalogClient.EligibilityResult(
            unitId, List.of(new GymCatalogClient.EligibilityItem(exerciseId, ExerciseKind.STRENGTH, false,
                Set.of("EQUIPMENT_UNAVAILABLE")))));

        assertThatThrownBy(() -> service.update("student-a", planId,
            new PlanModels.UpdatePlanRequest(0L, unitId, "Plano", List.of(requestDay(exerciseId))), "token"))
            .isInstanceOf(EligibilityConflictException.class);
        verify(transactions, never()).update(any(), any(), any(), any());
    }

    @Test
    void dependencyFailureClosesMutation() {
        UUID planId = UUID.randomUUID(), unitId = UUID.randomUUID(), exerciseId = UUID.randomUUID();
        when(transactions.owned("student-a", planId)).thenReturn(plan(planId, unitId, 0,
            List.of(day(exerciseId))));
        when(catalog.validate(any(), any(), any())).thenThrow(
            new DependencyUnavailableException("gym-service indisponível", new RuntimeException()));

        assertThatThrownBy(() -> service.update("student-a", planId,
            new PlanModels.UpdatePlanRequest(0L, unitId, "Plano", List.of(requestDay(exerciseId))), "token"))
            .isInstanceOf(DependencyUnavailableException.class);
        verify(transactions, never()).update(any(), any(), any(), any());
    }

    @Test
    void appliesSuggestionOnlyAfterRemoteChecksAndDelegatesTheShortWrite() {
        UUID planId = UUID.randomUUID(), unitId = UUID.randomUUID(), dayId = UUID.randomUUID();
        UUID currentExercise = UUID.randomUUID(), suggestedExercise = UUID.randomUUID(), suggestionId = UUID.randomUUID();
        PlanModels.DayResponse currentDay = day(dayId, currentExercise);
        PlanModels.PlanResponse current = plan(planId, unitId, 0, List.of(currentDay));
        when(transactions.previousApplication("student-a", "key-1", planId, suggestionId))
            .thenReturn(Optional.empty());
        when(transactions.owned("student-a", planId)).thenReturn(current);
        var change = new AssistantSuggestionClient.Change(AssistantSuggestionClient.Operation.ADD, dayId, null,
            suggestedExercise, 2, 3, 8, 12, null, 60, "Complemento");
        when(suggestions.get(suggestionId, "token")).thenReturn(new AssistantSuggestionClient.Suggestion(suggestionId,
            planId, 0, "fp", "AVAILABLE", "DEMO", "Explicação", List.of(), List.of(change), Instant.now(),
            Instant.now().plusSeconds(600)));
        when(catalog.validate(eq(unitId), any(), eq("token"))).thenReturn(new GymCatalogClient.EligibilityResult(unitId,
            List.of(new GymCatalogClient.EligibilityItem(currentExercise, ExerciseKind.STRENGTH, true, Set.of()),
                new GymCatalogClient.EligibilityItem(suggestedExercise, ExerciseKind.STRENGTH, true, Set.of()))));
        when(transactions.applySuggestion(eq("student-a"), eq(planId), eq(0L), eq("key-1"), eq(suggestionId), any()))
            .thenReturn(current);

        service.applySuggestion("student-a", planId,
            new PlanModels.ApplySuggestionRequest(suggestionId, 0L), "key-1", "token");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WorkoutDay>> days = ArgumentCaptor.forClass(List.class);
        verify(transactions).applySuggestion(eq("student-a"), eq(planId), eq(0L), eq("key-1"), eq(suggestionId),
            days.capture());
        assertThat(days.getValue().getFirst().getItems()).hasSize(2);
    }

    @Test
    void anIdempotentRetryDoesNotFetchOrApplyTheSuggestionAgain() {
        UUID planId = UUID.randomUUID(), unitId = UUID.randomUUID(), suggestionId = UUID.randomUUID();
        when(transactions.previousApplication("student-a", "stable-key", planId, suggestionId))
            .thenReturn(Optional.of(plan(planId, unitId, 1, List.of())));
        when(transactions.owned("student-a", planId)).thenReturn(plan(planId, unitId, 1, List.of()));

        service.applySuggestion("student-a", planId,
            new PlanModels.ApplySuggestionRequest(suggestionId, 0L), "stable-key", "token");

        verify(suggestions, never()).get(any(), any());
        verify(transactions, never()).applySuggestion(any(), any(), any(Long.class), any(), any(), any());
    }

    private PlanModels.PlanResponse plan(UUID id, UUID unitId, long version, List<PlanModels.DayResponse> days) {
        return new PlanModels.PlanResponse(id, unitId, "Plano", Goal.STRENGTH, Math.max(1, days.size()),
            PlanStatus.DRAFT, version, days, false, true, List.of(), Instant.now(), Instant.now());
    }

    private PlanModels.DayResponse day(UUID exerciseId) { return day(UUID.randomUUID(), exerciseId); }

    private PlanModels.DayResponse day(UUID dayId, UUID exerciseId) {
        return new PlanModels.DayResponse(dayId, 1, "Dia 1", List.of(new PlanModels.ItemResponse(UUID.randomUUID(),
            exerciseId, 1, 3, 8, 12, null, 60, null, null)));
    }

    private PlanModels.DayRequest requestDay(UUID exerciseId) {
        return new PlanModels.DayRequest(null, 1, "Dia 1", List.of(new PlanModels.ItemRequest(null, exerciseId, 1,
            3, 8, 12, null, 60, null, null)));
    }
}
