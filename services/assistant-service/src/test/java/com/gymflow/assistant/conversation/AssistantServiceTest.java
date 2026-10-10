package com.gymflow.assistant.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymflow.assistant.integration.AssistantContext;
import com.gymflow.assistant.integration.ContextService;
import com.gymflow.assistant.provider.ProviderOutputValidator;
import com.gymflow.assistant.provider.ProviderRouter;
import com.gymflow.assistant.provider.TrainingAssistantProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class AssistantServiceTest {
    @Test
    void generatesStructuredSuggestionForPlanWithoutBrowserPrompt() {
        AssistantRepository repository = mock(AssistantRepository.class);
        ContextService contexts = mock(ContextService.class);
        ProviderRouter providers = mock(ProviderRouter.class);
        ProviderOutputValidator validator = mock(ProviderOutputValidator.class);
        TrainingAssistantProvider provider = mock(TrainingAssistantProvider.class);
        UUID planId = UUID.randomUUID();
        UUID dayId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        AssistantContext context = new AssistantContext(
            new AssistantContext.Profile("STRENGTH", "BEGINNER", 3, 45, Set.of()),
            new AssistantContext.Plan(planId, UUID.randomUUID(), "Plano", "STRENGTH", 3, "DRAFT", 4,
                List.of(new AssistantContext.Day(dayId, 1, "Dia 1", List.of()))),
            List.of(), List.of(), "fingerprint");
        AssistantModels.SuggestedItem item = new AssistantModels.SuggestedItem(
            exerciseId, 1, 3, 8, 12, null, 60, "Complemento");
        AssistantModels.CompletionProposal completion = new AssistantModels.CompletionProposal(
            List.of(new AssistantModels.SuggestedDay(2, "Dia 2", List.of(item)),
                new AssistantModels.SuggestedDay(3, "Dia 3", List.of(item))),
            List.of(new AssistantModels.ExistingDayAddition(dayId, List.of(item))));
        TrainingAssistantProvider.CompletionResult result = new TrainingAssistantProvider.CompletionResult(
            "Sugestão estruturada", false, List.of(), completion);
        AssistantModels.SuggestionResponse saved = new AssistantModels.SuggestionResponse(
            UUID.randomUUID(), planId, 4, "fingerprint", "AVAILABLE", AssistantModels.Source.DEMO,
            AssistantModels.SuggestionKind.WORKOUT_COMPLETION, result.reply(), List.of(), List.of(), completion,
            Instant.now(), Instant.now().plusSeconds(600));

        when(contexts.build(eq(planId), eq("token"), eq(List.of()))).thenReturn(context);
        when(providers.selected()).thenReturn(provider);
        when(provider.complete(context)).thenReturn(result);
        when(validator.validateCompletion(result, context)).thenReturn(result);
        when(provider.source()).thenReturn(AssistantModels.Source.DEMO);
        when(repository.saveGeneratedCompletion(eq("student"), eq(planId), eq(4L),
            eq("fingerprint"), eq(AssistantModels.Source.DEMO), eq(result.reply()), eq(List.of()),
            eq(completion), any())).thenReturn(saved);

        AssistantService service = new AssistantService(repository, contexts, providers, validator,
            new AssistantRateLimiter(10, 1), new SimpleMeterRegistry(), Duration.ofMinutes(10), 10);

        assertThat(service.generateForPlan("student", planId, "token")).isEqualTo(saved);
        verify(repository).saveGeneratedCompletion(eq("student"), eq(planId), eq(4L),
            eq("fingerprint"), eq(AssistantModels.Source.DEMO), eq(result.reply()), eq(List.of()),
            eq(completion), any());
    }
}
