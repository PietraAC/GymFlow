package com.gymflow.assistant.provider;

import com.gymflow.assistant.conversation.AssistantModels;
import com.gymflow.assistant.integration.AssistantContext;
import com.gymflow.assistant.shared.error.ProviderResponseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProviderOutputValidator {
    public TrainingAssistantProvider.ProviderResult validate(TrainingAssistantProvider.ProviderResult result,
                                                              AssistantContext context) {
        if (result == null || result.reply() == null || result.reply().isBlank() || result.reply().length() > 2000) {
            throw invalid("Resposta textual ausente ou longa demais");
        }
        List<String> observations = result.observations() == null ? List.of() : result.observations();
        List<AssistantModels.SuggestionChange> changes = result.changes() == null ? List.of() : result.changes();
        if (observations.size() > 20 || observations.stream().anyMatch(value -> value == null || value.length() > 500)) {
            throw invalid("Observações inválidas");
        }
        if (changes.size() > 20 || result.needsProfessionalGuidance() && !changes.isEmpty()) {
            throw invalid("Quantidade ou combinação de mudanças inválida");
        }
        Map<UUID, AssistantContext.Day> days = context.plan().days().stream()
            .collect(Collectors.toMap(AssistantContext.Day::id, Function.identity()));
        Map<UUID, AssistantContext.Exercise> exercises = context.eligibleExercises().stream()
            .collect(Collectors.toMap(AssistantContext.Exercise::id, Function.identity()));
        Set<UUID> touched = new HashSet<>();
        for (AssistantModels.SuggestionChange change : changes) {
            if (change == null || change.operation() == null || change.dayId() == null) throw invalid("Operação incompleta");
            AssistantContext.Day day = days.get(change.dayId());
            if (day == null) throw invalid("A sugestão indicou um dia que não pertence ao plano");
            AssistantContext.Item target = change.targetItemId() == null ? null : day.items().stream()
                .filter(item -> item.id().equals(change.targetItemId())).findFirst().orElse(null);
            switch (change.operation()) {
                case ADD -> {
                    if (change.targetItemId() != null || change.exerciseId() == null || change.position() == null
                        || change.position() < 1 || change.position() > day.items().size() + 1) throw invalid("Operação ADD inválida");
                    validateExercise(change, exercises);
                }
                case REPLACE -> {
                    if (target == null || change.exerciseId() == null || !touched.add(change.targetItemId()))
                        throw invalid("Operação REPLACE inválida");
                    validateExercise(change, exercises);
                }
                case REMOVE -> {
                    if (target == null || change.exerciseId() != null || !touched.add(change.targetItemId()))
                        throw invalid("Operação REMOVE inválida");
                }
            }
            if (change.reason() == null || change.reason().isBlank() || change.reason().length() > 500) {
                throw invalid("Toda mudança precisa de uma justificativa curta");
            }
        }
        return new TrainingAssistantProvider.ProviderResult(result.reply().trim(), result.needsProfessionalGuidance(), observations, changes);
    }

    private void validateExercise(AssistantModels.SuggestionChange change, Map<UUID, AssistantContext.Exercise> exercises) {
        AssistantContext.Exercise exercise = exercises.get(change.exerciseId());
        if (exercise == null) throw invalid("A sugestão inventou ou usou um exercício inelegível");
        completeRange(change);
        if ("STRENGTH".equals(exercise.kind())) {
            if (change.sets() == null || change.repetitionMin() == null || change.repetitionMax() == null
                || change.durationSeconds() != null) throw invalid("Exercício de força com parâmetros inválidos");
        } else if (change.durationSeconds() == null && change.sets() == null) {
            throw invalid("Aquecimento ou alongamento sem duração ou repetições");
        }
        if (change.sets() != null && (change.sets() < 1 || change.sets() > 10)
            || change.repetitionMin() != null && (change.repetitionMin() < 1 || change.repetitionMin() > 100)
            || change.repetitionMax() != null && (change.repetitionMax() < 1 || change.repetitionMax() > 100)
            || change.durationSeconds() != null && (change.durationSeconds() < 5 || change.durationSeconds() > 1800)
            || change.restSeconds() != null && (change.restSeconds() < 0 || change.restSeconds() > 600)
            || change.repetitionMin() != null && change.repetitionMax() != null && change.repetitionMin() > change.repetitionMax()) {
            throw invalid("Parâmetros fora dos limites técnicos");
        }
    }

    private void completeRange(AssistantModels.SuggestionChange change) {
        int count = (change.sets() == null ? 0 : 1) + (change.repetitionMin() == null ? 0 : 1)
            + (change.repetitionMax() == null ? 0 : 1);
        if (count != 0 && count != 3) throw invalid("Séries e faixa de repetições devem estar completas");
    }

    private ProviderResponseException invalid(String message) { return new ProviderResponseException(message); }
}
