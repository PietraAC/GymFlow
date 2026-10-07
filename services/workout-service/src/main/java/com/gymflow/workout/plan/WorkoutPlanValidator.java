package com.gymflow.workout.plan;

import com.gymflow.workout.integration.gym.GymCatalogClient;
import com.gymflow.workout.shared.error.InvalidRequestException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class WorkoutPlanValidator {
    public void validateStructure(List<PlanModels.DayRequest> days, boolean activation) {
        if (activation && days.isEmpty()) throw new InvalidRequestException("Um plano ativo deve ter pelo menos um dia");
        requireUnique(days.stream().map(PlanModels.DayRequest::position).toList(), "As posições dos dias devem ser únicas");
        for (PlanModels.DayRequest day : days) {
            if (activation && day.items().isEmpty()) throw new InvalidRequestException("Todo dia de um plano ativo deve ter exercícios");
            requireUnique(day.items().stream().map(PlanModels.ItemRequest::position).toList(), "As posições dos exercícios de cada dia devem ser únicas");
            for (PlanModels.ItemRequest item : day.items()) {
                if (item.repetitionMin() != null && item.repetitionMax() != null && item.repetitionMin() > item.repetitionMax()) {
                    throw new InvalidRequestException("A repetição mínima não pode superar a máxima");
                }
            }
        }
    }

    public void validateKinds(List<PlanModels.DayRequest> days, GymCatalogClient.EligibilityResult eligibility) {
        Map<UUID, GymCatalogClient.EligibilityItem> byId = eligibility.results().stream()
            .collect(Collectors.toMap(GymCatalogClient.EligibilityItem::exerciseId, Function.identity()));
        for (PlanModels.ItemRequest item : days.stream().flatMap(day -> day.items().stream()).toList()) {
            GymCatalogClient.EligibilityItem catalogItem = byId.get(item.exerciseId());
            if (catalogItem == null || catalogItem.kind() == null) {
                throw new InvalidRequestException("O gym-service não informou a modalidade do exercício " + item.exerciseId());
            }
            validateItem(item, catalogItem.kind());
        }
    }

    public void validateItem(PlanModels.ItemRequest item, ExerciseKind kind) {
        boolean repetitionsComplete = item.sets() != null && item.repetitionMin() != null && item.repetitionMax() != null;
        if (kind == ExerciseKind.STRENGTH && (!repetitionsComplete || item.durationSeconds() != null)) {
            throw new InvalidRequestException("Exercícios de força exigem séries/repetições e não usam duração");
        }
        if (kind != ExerciseKind.STRENGTH && item.durationSeconds() == null && !repetitionsComplete) {
            throw new InvalidRequestException("Aquecimento e alongamento exigem duração ou séries/repetições completas");
        }
        if (!repetitionsComplete && (item.sets() != null || item.repetitionMin() != null || item.repetitionMax() != null)) {
            throw new InvalidRequestException("Séries e faixa de repetições devem ser informadas juntas");
        }
        if (kind != ExerciseKind.STRENGTH && item.optionalLoadKg() != null) {
            throw new InvalidRequestException("Carga opcional só pode ser informada para exercícios de força");
        }
    }

    private void requireUnique(List<Integer> positions, String message) {
        if (new HashSet<>(positions).size() != positions.size()) throw new InvalidRequestException(message);
    }
}
