package com.gymflow.assistant.conversation;

import com.gymflow.assistant.integration.AssistantContext;
import com.gymflow.assistant.integration.ContextService;
import com.gymflow.assistant.provider.ProviderOutputValidator;
import com.gymflow.assistant.provider.ProviderRouter;
import com.gymflow.assistant.provider.TrainingAssistantProvider;
import com.gymflow.assistant.shared.error.InvalidRequestException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AssistantService {
    private static final String DIRECT_SUGGESTION_INTENT = """
        Gere uma sugestão estruturada para complementar o rascunho atual usando somente exercícios elegíveis.
        Retorne apenas os dados do schema. Não faça perguntas, não crie dias e não altere carga em kg.
        """;
    private final AssistantRepository repository;
    private final ContextService contexts;
    private final ProviderRouter providers;
    private final ProviderOutputValidator validator;
    private final AssistantRateLimiter limiter;
    private final Duration suggestionTtl;
    private final int historyLimit;

    public AssistantService(AssistantRepository repository, ContextService contexts, ProviderRouter providers,
                            ProviderOutputValidator validator, AssistantRateLimiter limiter,
                            @Value("${app.ai.suggestion-ttl}") Duration suggestionTtl,
                            @Value("${app.ai.history-limit}") int historyLimit) {
        this.repository = repository; this.contexts = contexts; this.providers = providers; this.validator = validator;
        this.limiter = limiter; this.suggestionTtl = suggestionTtl; this.historyLimit = historyLimit;
    }

    public AssistantModels.ConversationResponse create(String subject, UUID planId, String token) {
        contexts.requireOwnedPlan(planId, token);
        return repository.createConversation(subject, planId);
    }

    public List<AssistantModels.MessageResponse> messages(String subject, UUID conversationId) {
        repository.ownedConversation(subject, conversationId);
        return repository.messages(conversationId);
    }

    public AssistantModels.AssistantTurnResponse send(String subject, UUID conversationId, String text, String token) {
        return limiter.execute(subject, () -> generate(subject, conversationId, text.trim(), token));
    }

    public AssistantModels.SuggestionResponse suggestion(String subject, UUID suggestionId) {
        return repository.ownedSuggestion(subject, suggestionId);
    }

    public AssistantModels.SuggestionResponse generateForPlan(String subject, UUID planId, String token) {
        return limiter.execute(subject, () -> {
            AssistantContext context = contexts.build(planId, token, List.of());
            TrainingAssistantProvider provider = providers.selected();
            TrainingAssistantProvider.ProviderResult result = validator.validate(
                provider.generate(context, DIRECT_SUGGESTION_INTENT), context);
            if (result.changes().isEmpty()) {
                throw new InvalidRequestException("Não foi encontrada uma alteração elegível para este rascunho");
            }
            AssistantModels.ConversationResponse conversation = repository.createConversation(subject, planId);
            return repository.saveSuggestion(conversation.id(), subject, planId, context.plan().version(),
                context.fingerprint(), provider.source(), result.reply(), result.observations(), result.changes(),
                Instant.now().plus(suggestionTtl));
        });
    }

    private AssistantModels.AssistantTurnResponse generate(String subject, UUID conversationId, String text, String token) {
        AssistantModels.ConversationResponse conversation = repository.ownedConversation(subject, conversationId);
        List<AssistantModels.MessageResponse> previous = repository.messages(conversationId);
        List<AssistantModels.MessageResponse> history = previous.stream()
            .skip(Math.max(0, previous.size() - historyLimit)).toList();
        repository.addMessage(conversationId, "USER", text);
        AssistantContext context = contexts.build(conversation.planId(), token, history);
        TrainingAssistantProvider provider = providers.selected();
        TrainingAssistantProvider.ProviderResult result = validator.validate(provider.generate(context, text), context);
        AssistantModels.MessageResponse assistantMessage = repository.addMessage(conversationId, "ASSISTANT", result.reply());
        AssistantModels.SuggestionResponse suggestion = result.changes().isEmpty() ? null : repository.saveSuggestion(
            conversationId, subject, conversation.planId(), context.plan().version(), context.fingerprint(), provider.source(),
            result.reply(), result.observations(), result.changes(), Instant.now().plus(suggestionTtl));
        return new AssistantModels.AssistantTurnResponse(assistantMessage, result.needsProfessionalGuidance(),
            result.observations(), suggestion);
    }
}
