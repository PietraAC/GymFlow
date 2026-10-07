package com.gymflow.gym.eligibility;

import com.gymflow.gym.equipment.UnitEquipment;
import com.gymflow.gym.equipment.UnitEquipmentRepository;
import com.gymflow.gym.exercise.Difficulty;
import com.gymflow.gym.exercise.Exercise;
import com.gymflow.gym.exercise.ExerciseKind;
import com.gymflow.gym.exercise.ExerciseModels;
import com.gymflow.gym.exercise.ExerciseRepository;
import com.gymflow.gym.shared.api.PageResponse;
import com.gymflow.gym.shared.error.InvalidRequestException;
import com.gymflow.gym.unit.UnitApplicationService;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EligibilityService {
    private final ExerciseRepository exercises;
    private final UnitEquipmentRepository inventory;
    private final UnitApplicationService units;
    private final EligibilityEvaluator evaluator;

    public EligibilityService(ExerciseRepository exercises, UnitEquipmentRepository inventory,
                              UnitApplicationService units, EligibilityEvaluator evaluator) {
        this.exercises = exercises;
        this.inventory = inventory;
        this.units = units;
        this.evaluator = evaluator;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExerciseModels.ExerciseResponse> eligible(UUID unitId, String subject, String muscleGroup,
                                                                   ExerciseKind kind, Difficulty difficulty,
                                                                   int page, int size) {
        validatePage(page, size);
        units.requireVisible(unitId, subject);
        Map<UUID, UnitEquipment> inventoryMap = inventory(unitId);
        String group = muscleGroup == null ? null : muscleGroup.trim().toUpperCase();
        List<ExerciseModels.ExerciseResponse> eligible = exercises.findByActiveTrueOrderByNameAsc().stream()
            .filter(exercise -> kind == null || exercise.getKind() == kind)
            .filter(exercise -> difficulty == null || exercise.getDifficulty() == difficulty)
            .filter(exercise -> group == null || group.isBlank() || exercise.getPrimaryMuscleGroups().contains(group)
                || exercise.getSecondaryMuscleGroups().contains(group))
            .filter(exercise -> evaluator.evaluate(exercise, inventoryMap).eligible())
            .map(ExerciseModels.ExerciseResponse::from).toList();
        int from = Math.min(page * size, eligible.size());
        int to = Math.min(from + size, eligible.size());
        int pages = eligible.isEmpty() ? 0 : (int) Math.ceil((double) eligible.size() / size);
        return new PageResponse<>(eligible.subList(from, to), page, size, eligible.size(), pages);
    }

    @Transactional(readOnly = true)
    public EligibilityModels.EligibilityResponse validate(UUID unitId, String subject, Set<UUID> exerciseIds) {
        units.requireVisible(unitId, subject);
        Map<UUID, UnitEquipment> inventoryMap = inventory(unitId);
        Map<UUID, Exercise> found = exercises.findByIdIn(exerciseIds).stream()
            .collect(Collectors.toMap(Exercise::getId, Function.identity()));
        List<EligibilityModels.EligibilityItem> results = exerciseIds.stream().sorted(Comparator.naturalOrder()).map(id -> {
            Exercise exercise = found.get(id);
            if (exercise == null || !exercise.isActive()) {
                return new EligibilityModels.EligibilityItem(id, null, false, Set.of(EligibilityReason.EXERCISE_NOT_FOUND_OR_INACTIVE));
            }
            return EligibilityModels.EligibilityItem.from(evaluator.evaluate(exercise, inventoryMap), exercise.getKind());
        }).toList();
        return new EligibilityModels.EligibilityResponse(unitId, results);
    }

    private Map<UUID, UnitEquipment> inventory(UUID unitId) {
        return inventory.findByUnitIdOrderByEquipmentTypeId(unitId).stream().collect(Collectors.toMap(
            UnitEquipment::getEquipmentTypeId, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new InvalidRequestException("page >= 0 e size entre 1 e 100");
    }
}
