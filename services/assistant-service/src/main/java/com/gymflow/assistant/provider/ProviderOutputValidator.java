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

    public TrainingAssistantProvider.CompletionResult validateCompletion(
        TrainingAssistantProvider.CompletionResult result, AssistantContext context) {
        if (result == null || result.reply() == null || result.reply().isBlank() || result.reply().length() > 2000) {
            throw invalid("Resposta textual ausente ou longa demais");
        }
        List<String> observations = result.observations() == null ? List.of() : result.observations();
        if (observations.size() > 20 || observations.stream().anyMatch(value -> value == null || value.length() > 500)) {
            throw invalid("Observações inválidas");
        }
        if (result.needsProfessionalGuidance() || result.completion() == null) {
            throw invalid("A conclusão automática precisa retornar uma proposta aplicável");
        }
        List<AssistantModels.SuggestedDay> newDays = result.completion().newDays() == null
            ? List.of() : result.completion().newDays();
        List<AssistantModels.ExistingDayAddition> additions = result.completion().existingDayAdditions() == null
            ? List.of() : result.completion().existingDayAdditions();
        int currentDays = context.plan().days().size();
        int targetDays = context.plan().targetDaysPerWeek();
        if (currentDays > targetDays || newDays.size() != targetDays - currentDays) {
            throw invalid("A proposta deve completar exatamente a quantidade de dias definida no plano");
        }
        Map<UUID, AssistantContext.Day> days = context.plan().days().stream()
            .collect(Collectors.toMap(AssistantContext.Day::id, Function.identity()));
        Map<UUID, AssistantContext.Exercise> exercises = context.eligibleExercises().stream()
            .collect(Collectors.toMap(AssistantContext.Exercise::id, Function.identity()));
        Set<UUID> touchedDays = new HashSet<>();
        int itemCount = 0;
        for (AssistantModels.ExistingDayAddition addition : additions) {
            if (addition == null || addition.dayId() == null || !touchedDays.add(addition.dayId()) || !days.containsKey(addition.dayId())) {
                throw invalid("A proposta indicou um dia existente inválido ou repetido");
            }
            AssistantContext.Day day = days.get(addition.dayId());
            List<AssistantModels.SuggestedItem> items = safeItems(addition.items());
            if (day.items().isEmpty() && items.isEmpty()) {
                throw invalid("Todo dia vazio precisa receber exercícios na proposta");
            }
            validateItems(items, exercises, day.items().stream().map(AssistantContext.Item::exerciseId).collect(Collectors.toSet()),
                day.items().size() + 1);
            itemCount += items.size();
        }
        for (AssistantContext.Day day : context.plan().days()) {
            if (day.items().isEmpty() && !touchedDays.contains(day.id())) {
                throw invalid("Todo dia vazio precisa receber exercícios na proposta");
            }
        }
        Set<Integer> positions = new HashSet<>();
        for (AssistantModels.SuggestedDay day : newDays) {
            if (day == null || day.position() == null || day.position() < 1 || day.position() > targetDays
                || !positions.add(day.position()) || day.name() == null || day.name().isBlank() || day.name().length() > 80) {
                throw invalid("Novo dia incompleto ou inválido");
            }
            List<AssistantModels.SuggestedItem> items = safeItems(day.items());
            if (items.isEmpty()) throw invalid("Todo novo dia precisa ter exercícios");
            validateItems(items, exercises, Set.of(), 1);
            itemCount += items.size();
        }
        for (int position = currentDays + 1; position <= targetDays; position++) {
            if (!positions.contains(position)) throw invalid("As posições dos novos dias devem ser contíguas");
        }
        if (itemCount == 0 || itemCount > 60) throw invalid("Quantidade de exercícios sugeridos inválida");
        AssistantModels.CompletionProposal completion = new AssistantModels.CompletionProposal(newDays, additions);
        return new TrainingAssistantProvider.CompletionResult(result.reply().trim(), false, observations, completion);
    }

    private List<AssistantModels.SuggestedItem> safeItems(List<AssistantModels.SuggestedItem> items) {
        return items == null ? List.of() : items;
    }

    private void validateItems(List<AssistantModels.SuggestedItem> items,
                               Map<UUID, AssistantContext.Exercise> exercises, Set<UUID> existing,
                               int firstPosition) {
        if (items.size() > 12) throw invalid("Um dia não pode receber mais de 12 exercícios");
        Set<UUID> exerciseIds = new HashSet<>(existing);
        Set<Integer> positions = new HashSet<>();
        for (int index = 0; index < items.size(); index++) {
            AssistantModels.SuggestedItem item = items.get(index);
            int expectedPosition = firstPosition + index;
            if (item == null || item.exerciseId() == null || item.position() == null
                || item.position() != expectedPosition || !positions.add(item.position())
                || !exerciseIds.add(item.exerciseId())) {
                throw invalid("Item sugerido incompleto, repetido ou fora de ordem");
            }
            validateSuggestedItem(item, exercises);
            if (item.reason() == null || item.reason().isBlank() || item.reason().length() > 500) {
                throw invalid("Todo exercício sugerido precisa de uma justificativa curta");
            }
        }
    }

    private void validateSuggestedItem(AssistantModels.SuggestedItem item,
                                       Map<UUID, AssistantContext.Exercise> exercises) {
        AssistantContext.Exercise exercise = exercises.get(item.exerciseId());
        if (exercise == null) throw invalid("A sugestão inventou ou usou um exercício inelegível");
        int count = (item.sets() == null ? 0 : 1) + (item.repetitionMin() == null ? 0 : 1)
            + (item.repetitionMax() == null ? 0 : 1);
        if (count != 0 && count != 3) throw invalid("Séries e faixa de repetições devem estar completas");
        if ("STRENGTH".equals(exercise.kind())) {
            if (count != 3 || item.durationSeconds() != null) throw invalid("Exercício de força com parâmetros inválidos");
        } else if ((item.durationSeconds() == null) == (count == 0)) {
            throw invalid("Aquecimento e alongamento exigem duração ou repetições completas, mas não ambos");
        }
        if (item.sets() != null && (item.sets() < 1 || item.sets() > 10)
            || item.repetitionMin() != null && (item.repetitionMin() < 1 || item.repetitionMin() > 100)
            || item.repetitionMax() != null && (item.repetitionMax() < 1 || item.repetitionMax() > 100)
            || item.durationSeconds() != null && (item.durationSeconds() < 5 || item.durationSeconds() > 1800)
            || item.restSeconds() != null && (item.restSeconds() < 0 || item.restSeconds() > 600)
            || item.repetitionMin() != null && item.repetitionMax() != null && item.repetitionMin() > item.repetitionMax()) {
            throw invalid("Parâmetros fora dos limites técnicos");
        }
    }

    private void validateExercise(AssistantModels.SuggestionChange change, Map<UUID, AssistantContext.Exercise> exercises) {
        AssistantContext.Exercise exercise = exercises.get(change.exerciseId());
        if (exercise == null) throw invalid("A sugestão inventou ou usou um exercício inelegível");
        completeRange(change);
        if ("STRENGTH".equals(exercise.kind())) {
            if (change.sets() == null || change.repetitionMin() == null || change.repetitionMax() == null
                || change.durationSeconds() != null) throw invalid("Exercício de força com parâmetros inválidos");
        } else if ((change.durationSeconds() == null) == (change.sets() == null)) {
            throw invalid("Aquecimento e alongamento exigem duração ou repetições completas, mas não ambos");
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
