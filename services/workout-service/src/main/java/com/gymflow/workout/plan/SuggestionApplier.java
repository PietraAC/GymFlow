package com.gymflow.workout.plan;

import com.gymflow.workout.integration.assistant.AssistantSuggestionClient;
import com.gymflow.workout.shared.error.ConflictException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SuggestionApplier {
    public List<PlanModels.DayRequest> apply(List<PlanModels.DayRequest> original,
                                             AssistantSuggestionClient.Suggestion suggestion,
                                             int targetDaysPerWeek) {
        return suggestion.kind() == AssistantSuggestionClient.SuggestionKind.WORKOUT_COMPLETION
            ? applyCompletion(original, suggestion.completion(), targetDaysPerWeek)
            : applyChanges(original, suggestion.changes());
    }

    private List<PlanModels.DayRequest> applyChanges(List<PlanModels.DayRequest> original,
                                                      List<AssistantSuggestionClient.Change> changes) {
        List<PlanModels.DayRequest> result = new ArrayList<>(original);
        for (AssistantSuggestionClient.Change change : changes) {
            int dayIndex = indexOfDay(result, change.dayId());
            PlanModels.DayRequest day = result.get(dayIndex);
            List<PlanModels.ItemRequest> items = new ArrayList<>(day.items());
            int targetIndex = change.targetItemId() == null ? -1 : indexOfItem(items, change.targetItemId());
            switch (change.operation()) {
                case ADD -> {
                    int insert = change.position() == null ? items.size() : change.position() - 1;
                    if (insert < 0 || insert > items.size() || change.exerciseId() == null) {
                        throw new ConflictException("Operação ADD inválida");
                    }
                    items.add(insert, suggestedItem(null, change, insert + 1));
                }
                case REPLACE -> {
                    if (targetIndex < 0 || change.exerciseId() == null) throw new ConflictException("Operação REPLACE inválida");
                    items.set(targetIndex, suggestedItem(items.get(targetIndex).id(), change, targetIndex + 1));
                }
                case REMOVE -> {
                    if (targetIndex < 0) throw new ConflictException("Operação REMOVE inválida");
                    items.remove(targetIndex);
                }
            }
            result.set(dayIndex, new PlanModels.DayRequest(day.id(), day.position(), day.name(), normalize(items)));
        }
        return result;
    }

    private List<PlanModels.DayRequest> applyCompletion(List<PlanModels.DayRequest> original,
                                                         AssistantSuggestionClient.Completion completion,
                                                         int targetDaysPerWeek) {
        if (completion == null) throw new ConflictException("A sugestão de treino completo não possui conteúdo");
        List<PlanModels.DayRequest> result = new ArrayList<>(original);
        for (AssistantSuggestionClient.ExistingDayAddition addition : safe(completion.existingDayAdditions())) {
            int dayIndex = indexOfDay(result, addition.dayId());
            PlanModels.DayRequest day = result.get(dayIndex);
            List<PlanModels.ItemRequest> items = new ArrayList<>(day.items());
            for (AssistantSuggestionClient.SuggestedItem item : safe(addition.items())) {
                items.add(suggestedItem(item, items.size() + 1));
            }
            result.set(dayIndex, new PlanModels.DayRequest(day.id(), day.position(), day.name(), items));
        }
        for (AssistantSuggestionClient.SuggestedDay day : safe(completion.newDays())) {
            List<PlanModels.ItemRequest> items = new ArrayList<>();
            for (AssistantSuggestionClient.SuggestedItem item : safe(day.items())) {
                items.add(suggestedItem(item, items.size() + 1));
            }
            result.add(new PlanModels.DayRequest(null, day.position(), day.name(), items));
        }
        if (result.size() != targetDaysPerWeek) {
            throw new ConflictException("A sugestão não completa a frequência semanal definida");
        }
        return result;
    }

    private int indexOfDay(List<PlanModels.DayRequest> days, UUID id) {
        for (int index = 0; index < days.size(); index++) {
            if (days.get(index).id().equals(id)) return index;
        }
        throw new ConflictException("A sugestão referencia um dia inexistente");
    }

    private int indexOfItem(List<PlanModels.ItemRequest> items, UUID id) {
        for (int index = 0; index < items.size(); index++) {
            if (items.get(index).id().equals(id)) return index;
        }
        return -1;
    }

    private List<PlanModels.ItemRequest> normalize(List<PlanModels.ItemRequest> items) {
        List<PlanModels.ItemRequest> normalized = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            PlanModels.ItemRequest item = items.get(index);
            normalized.add(new PlanModels.ItemRequest(item.id(), item.exerciseId(), index + 1, item.sets(),
                item.repetitionMin(), item.repetitionMax(), item.durationSeconds(), item.restSeconds(),
                item.optionalLoadKg(), item.notes()));
        }
        return normalized;
    }

    private PlanModels.ItemRequest suggestedItem(UUID id, AssistantSuggestionClient.Change change, int position) {
        return new PlanModels.ItemRequest(id, change.exerciseId(), position, change.sets(), change.repetitionMin(),
            change.repetitionMax(), change.durationSeconds(), change.restSeconds(), null, null);
    }

    private PlanModels.ItemRequest suggestedItem(AssistantSuggestionClient.SuggestedItem item, int position) {
        return new PlanModels.ItemRequest(null, item.exerciseId(), position, item.sets(), item.repetitionMin(),
            item.repetitionMax(), item.durationSeconds(), item.restSeconds(), null, null);
    }

    private <T> List<T> safe(List<T> values) { return values == null ? List.of() : values; }
}
