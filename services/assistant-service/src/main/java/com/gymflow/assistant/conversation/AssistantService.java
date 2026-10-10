package com.gymflow.assistant.conversation;

import com.gymflow.assistant.integration.AssistantContext;
import com.gymflow.assistant.integration.ContextService;
import com.gymflow.assistant.provider.ProviderOutputValidator;
import com.gymflow.assistant.provider.ProviderRouter;
import com.gymflow.assistant.provider.TrainingAssistantProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AssistantService {
    private final AssistantRepository repository;
    private final ContextService contexts;
    private final ProviderRouter providers;
    private final ProviderOutputValidator validator;
    private final AssistantRateLimiter limiter;
    private final MeterRegistry metrics;
    private final Duration suggestionTtl;
    private final int historyLimit;

    public AssistantService(AssistantRepository repository, ContextService contexts, ProviderRouter providers,
                            ProviderOutputValidator validator, AssistantRateLimiter limiter,
                            MeterRegistry metrics,
                            @Value("${app.ai.suggestion-ttl}") Duration suggestionTtl,
                            @Value("${app.ai.history-limit}") int historyLimit) {
        this.repository = repository; this.contexts = contexts; this.providers = providers; this.validator = validator;
        this.limiter = limiter; this.metrics = metrics; this.suggestionTtl = suggestionTtl; this.historyLimit = historyLimit;
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
            Timer.Sample sample = Timer.start(metrics);
            String source = "unselected";
            try {
                AssistantContext context = contexts.build(planId, token, List.of());
                TrainingAssistantProvider provider = providers.selected();
                source = provider.source().name().toLowerCase();
                TrainingAssistantProvider.CompletionResult result = validator.validateCompletion(
                    provider.complete(context), context);
                AssistantModels.SuggestionResponse suggestion = repository.saveGeneratedCompletion(
                    subject, planId, context.plan().version(), context.fingerprint(), provider.source(),
                    result.reply(), result.observations(), result.completion(), Instant.now().plus(suggestionTtl));
                int changeCount = result.completion().newDays().stream().mapToInt(day -> day.items().size()).sum()
                    + result.completion().existingDayAdditions().stream().mapToInt(day -> day.items().size()).sum();
                metrics.summary("gymflow.ai.suggestion.changes", "source", source)
                    .record(changeCount);
                recordSuggestion(sample, source, "success");
                return suggestion;
            } catch (RuntimeException exception) {
                recordSuggestion(sample, source, "failure");
                throw exception;
            }
        });
    }

    private void recordSuggestion(Timer.Sample sample, String source, String outcome) {
        metrics.counter("gymflow.ai.suggestions", "source", source, "outcome", outcome).increment();
        sample.stop(metrics.timer("gymflow.ai.suggestion.duration", "source", source, "outcome", outcome));
    }

    private AssistantModels.AssistantTurnResponse generate(String subject, UUID conversationId, String text, String token) {
        AssistantModels.ConversationResponse conversation = repository.ownedConversation(subject, conversationId);
        List<AssistantModels.MessageResponse> previous = repository.messages(conversationId);
        List<AssistantModels.MessageResponse> history = previous.stream()
            .skip(Math.max(0, previous.size() - historyLimit)).toList();
        AssistantContext context = contexts.build(conversation.planId(), token, history);
        TrainingAssistantProvider provider = providers.selected();
        TrainingAssistantProvider.ProviderResult result = validator.validate(provider.generate(context, text), context);
        AssistantRepository.SavedTurn saved = repository.saveTurn(conversationId, subject, conversation.planId(),
            context.plan().version(), context.fingerprint(), provider.source(), text, result.reply(),
            result.observations(), result.changes(), Instant.now().plus(suggestionTtl));
        return new AssistantModels.AssistantTurnResponse(saved.assistant(), result.needsProfessionalGuidance(),
            result.observations(), saved.suggestion());
    }
}
