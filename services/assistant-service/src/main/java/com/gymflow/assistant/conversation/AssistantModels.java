package com.gymflow.assistant.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AssistantModels {
    private AssistantModels() {}

    public record CreateConversationRequest(@NotNull UUID planId) {}
    public record SendMessageRequest(@NotBlank @Size(max = 2000) String text) {}
    public record ConversationResponse(UUID id, UUID planId, Instant createdAt) {}
    public record MessageResponse(UUID id, String role, String text, Instant createdAt) {}

    public enum Operation { ADD, REPLACE, REMOVE }
    public enum Source { DEMO, GEMINI }
    public enum SuggestionKind { ITEM_CHANGES, WORKOUT_COMPLETION }

    public record SuggestionChange(Operation operation, UUID dayId, UUID targetItemId, UUID exerciseId,
                                   Integer position, Integer sets, Integer repetitionMin, Integer repetitionMax,
                                   Integer durationSeconds, Integer restSeconds, String reason) {}

    public record SuggestedItem(UUID exerciseId, Integer position, Integer sets, Integer repetitionMin,
                                Integer repetitionMax, Integer durationSeconds, Integer restSeconds, String reason) {}
    public record SuggestedDay(Integer position, String name, List<SuggestedItem> items) {}
    public record ExistingDayAddition(UUID dayId, List<SuggestedItem> items) {}
    public record CompletionProposal(List<SuggestedDay> newDays, List<ExistingDayAddition> existingDayAdditions) {}

    public record SuggestionResponse(UUID id, UUID planId, long basePlanVersion, String contextFingerprint,
                                     String status, Source source, SuggestionKind kind, String explanation,
                                     List<String> observations, List<SuggestionChange> changes,
                                     CompletionProposal completion, Instant createdAt, Instant expiresAt) {
        public SuggestionResponse(UUID id, UUID planId, long basePlanVersion, String contextFingerprint,
                                  String status, Source source, String explanation, List<String> observations,
                                  List<SuggestionChange> changes, Instant createdAt, Instant expiresAt) {
            this(id, planId, basePlanVersion, contextFingerprint, status, source, SuggestionKind.ITEM_CHANGES,
                explanation, observations, changes, null, createdAt, expiresAt);
        }
    }

    public record AssistantTurnResponse(MessageResponse message, boolean needsProfessionalGuidance,
                                        List<String> observations, SuggestionResponse suggestion) {}
}
