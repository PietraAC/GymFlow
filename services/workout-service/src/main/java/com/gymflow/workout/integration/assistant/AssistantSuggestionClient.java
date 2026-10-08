package com.gymflow.workout.integration.assistant;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AssistantSuggestionClient {
    Suggestion get(UUID id, String bearerToken);
    enum Operation { ADD, REPLACE, REMOVE }
    enum SuggestionKind { ITEM_CHANGES, WORKOUT_COMPLETION }
    record Change(Operation operation, UUID dayId, UUID targetItemId, UUID exerciseId, Integer position,
                  Integer sets, Integer repetitionMin, Integer repetitionMax, Integer durationSeconds,
                  Integer restSeconds, String reason) {}
    record SuggestedItem(UUID exerciseId, Integer position, Integer sets, Integer repetitionMin,
                         Integer repetitionMax, Integer durationSeconds, Integer restSeconds, String reason) {}
    record SuggestedDay(Integer position, String name, List<SuggestedItem> items) {}
    record ExistingDayAddition(UUID dayId, List<SuggestedItem> items) {}
    record Completion(List<SuggestedDay> newDays, List<ExistingDayAddition> existingDayAdditions) {}
    record Suggestion(UUID id, UUID planId, long basePlanVersion, String contextFingerprint, String status,
                      String source, SuggestionKind kind, String explanation, List<String> observations,
                      List<Change> changes, Completion completion, Instant createdAt, Instant expiresAt) {
        public Suggestion(UUID id, UUID planId, long basePlanVersion, String contextFingerprint, String status,
                          String source, String explanation, List<String> observations, List<Change> changes,
                          Instant createdAt, Instant expiresAt) {
            this(id, planId, basePlanVersion, contextFingerprint, status, source, SuggestionKind.ITEM_CHANGES,
                explanation, observations, changes, null, createdAt, expiresAt);
        }
    }
}
