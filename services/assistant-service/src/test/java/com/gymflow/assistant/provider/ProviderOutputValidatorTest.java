package com.gymflow.assistant.provider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymflow.assistant.conversation.AssistantModels;
import com.gymflow.assistant.integration.AssistantContext;
import com.gymflow.assistant.shared.error.ProviderResponseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProviderOutputValidatorTest {
    private final ProviderOutputValidator validator = new ProviderOutputValidator();

    @Test void rejectsInventedExerciseIds() {
        UUID dayId = UUID.randomUUID();
        AssistantContext context = new AssistantContext(new AssistantContext.Profile("STRENGTH", "BEGINNER", 3, 45, Set.of()),
            new AssistantContext.Plan(UUID.randomUUID(), UUID.randomUUID(), "Plano", "DRAFT", 1,
                List.of(new AssistantContext.Day(dayId, 1, "Dia", List.of()))), List.of(), List.of(), "fp");
        var change = new AssistantModels.SuggestionChange(AssistantModels.Operation.ADD, dayId, null, UUID.randomUUID(),
            1, 3, 8, 12, null, 60, "Razão");
        var result = new TrainingAssistantProvider.ProviderResult("Resposta", false, List.of(), List.of(change));
        assertThatThrownBy(() -> validator.validate(result, context)).isInstanceOf(ProviderResponseException.class)
            .hasMessageContaining("inventou");
    }

    @Test void rejectsChangesWhenProfessionalGuidanceIsRequired() {
        var result = new TrainingAssistantProvider.ProviderResult("Procure ajuda", true, List.of(),
            List.of(new AssistantModels.SuggestionChange(AssistantModels.Operation.REMOVE, UUID.randomUUID(),
                UUID.randomUUID(), null, null, null, null, null, null, null, "Razão")));
        AssistantContext context = new AssistantContext(null, null, List.of(), List.of(), "fp");
        assertThatThrownBy(() -> validator.validate(result, context)).isInstanceOf(ProviderResponseException.class);
    }

    @Test void rejectsTargetItemFromAnotherDay() {
        UUID firstDay = UUID.randomUUID(), secondDay = UUID.randomUUID(), foreignItem = UUID.randomUUID();
        var context = new AssistantContext(new AssistantContext.Profile("STRENGTH", "BEGINNER", 3, 45, Set.of()),
            new AssistantContext.Plan(UUID.randomUUID(), UUID.randomUUID(), "Plano", "DRAFT", 1, List.of(
                new AssistantContext.Day(firstDay, 1, "A", List.of()),
                new AssistantContext.Day(secondDay, 2, "B", List.of(new AssistantContext.Item(foreignItem,
                    UUID.randomUUID(), 1, 3, 8, 12, null, 60, null))))), List.of(), List.of(), "fp");
        var change = new AssistantModels.SuggestionChange(AssistantModels.Operation.REMOVE, firstDay, foreignItem,
            null, null, null, null, null, null, null, "Remover");
        var result = new TrainingAssistantProvider.ProviderResult("Resposta", false, List.of(), List.of(change));
        assertThatThrownBy(() -> validator.validate(result, context)).isInstanceOf(ProviderResponseException.class)
            .hasMessageContaining("REMOVE");
    }

    @Test void rejectsInvalidAddShape() {
        UUID dayId = UUID.randomUUID(), exerciseId = UUID.randomUUID();
        var exercise = new AssistantContext.Exercise(exerciseId, "Exercicio", "STRENGTH", Set.of(), Set.of(),
            "PUSH", "BEGINNER", "Instrucao", List.of());
        var context = new AssistantContext(new AssistantContext.Profile("STRENGTH", "BEGINNER", 3, 45, Set.of()),
            new AssistantContext.Plan(UUID.randomUUID(), UUID.randomUUID(), "Plano", "DRAFT", 1,
                List.of(new AssistantContext.Day(dayId, 1, "A", List.of()))), List.of(exercise), List.of(), "fp");
        var change = new AssistantModels.SuggestionChange(AssistantModels.Operation.ADD, dayId, UUID.randomUUID(),
            exerciseId, 1, 3, 8, 12, null, 60, "Adicionar");
        var result = new TrainingAssistantProvider.ProviderResult("Resposta", false, List.of(), List.of(change));
        assertThatThrownBy(() -> validator.validate(result, context)).isInstanceOf(ProviderResponseException.class)
            .hasMessageContaining("ADD");
    }

    @Test void validatesACompleteWeekAndRejectsInventedCatalogIds() {
        UUID dayId = UUID.randomUUID(), exerciseId = UUID.randomUUID();
        var exercise = new AssistantContext.Exercise(exerciseId, "Exercicio", "STRENGTH", Set.of(), Set.of(),
            "PUSH", "BEGINNER", "Instrucao", List.of());
        var context = new AssistantContext(new AssistantContext.Profile("STRENGTH", "BEGINNER", 3, 45, Set.of()),
            new AssistantContext.Plan(UUID.randomUUID(), UUID.randomUUID(), "Plano", "STRENGTH", 3, "DRAFT", 1,
                List.of(new AssistantContext.Day(dayId, 1, "A", List.of()))), List.of(exercise), List.of(), "fp");
        var valid = new AssistantModels.SuggestedItem(exerciseId, 1, 3, 8, 12, null, 60, "Razao");
        var invented = new AssistantModels.SuggestedItem(UUID.randomUUID(), 1, 3, 8, 12, null, 60, "Razao");
        var completion = new AssistantModels.CompletionProposal(
            List.of(new AssistantModels.SuggestedDay(2, "B", List.of(valid)),
                new AssistantModels.SuggestedDay(3, "C", List.of(invented))),
            List.of(new AssistantModels.ExistingDayAddition(dayId, List.of(valid))));
        var result = new TrainingAssistantProvider.CompletionResult("Resposta", false, List.of(), completion);

        assertThatThrownBy(() -> validator.validateCompletion(result, context))
            .isInstanceOf(ProviderResponseException.class).hasMessageContaining("inventou");
    }
}
