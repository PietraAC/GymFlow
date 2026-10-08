package com.gymflow.workout.integration.assistant;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AssistantSuggestionClient {
    Suggestion get(UUID id, String bearerToken);
    enum Operation { ADD, REPLACE, REMOVE }
    record Change(Operation operation, UUID dayId, UUID targetItemId, UUID exerciseId, Integer position,
                  Integer sets, Integer repetitionMin, Integer repetitionMax, Integer durationSeconds,
                  Integer restSeconds, String reason) {}
    record Suggestion(UUID id, UUID planId, long basePlanVersion, String contextFingerprint, String status,
                      String source, String explanation, List<String> observations, List<Change> changes,
                      Instant createdAt, Instant expiresAt) {}
}
