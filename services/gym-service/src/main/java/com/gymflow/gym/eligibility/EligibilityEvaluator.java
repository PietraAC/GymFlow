package com.gymflow.gym.eligibility;

import com.gymflow.gym.equipment.UnitEquipment;
import com.gymflow.gym.exercise.Exercise;
import com.gymflow.gym.exercise.ExerciseRequirementOption;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EligibilityEvaluator {
    public EligibilityDecision evaluate(Exercise exercise, Map<UUID, UnitEquipment> inventory) {
        if (!exercise.isActive()) {
            return new EligibilityDecision(exercise.getId(), false, Set.of(EligibilityReason.EXERCISE_NOT_FOUND_OR_INACTIVE));
        }
        Set<ExerciseRequirementOption> options = exercise.getRequirementOptions();
        if (options.isEmpty() || options.stream().anyMatch(option -> option.getRequiredEquipmentTypes().isEmpty())) {
            return new EligibilityDecision(exercise.getId(), true, Set.of());
        }

        EnumSet<EligibilityReason> aggregateReasons = EnumSet.noneOf(EligibilityReason.class);
        for (ExerciseRequirementOption option : options) {
            EnumSet<EligibilityReason> optionReasons = EnumSet.noneOf(EligibilityReason.class);
            option.getRequiredEquipmentTypes().forEach(type -> {
                if (!type.isActive()) {
                    optionReasons.add(EligibilityReason.EQUIPMENT_TYPE_INACTIVE);
                    return;
                }
                UnitEquipment item = inventory.get(type.getId());
                if (item == null) optionReasons.add(EligibilityReason.MISSING_EQUIPMENT);
                else if (item.getAvailableQuantity() <= 0) optionReasons.add(EligibilityReason.EQUIPMENT_UNAVAILABLE);
            });
            if (optionReasons.isEmpty()) {
                return new EligibilityDecision(exercise.getId(), true, Set.of());
            }
            aggregateReasons.addAll(optionReasons);
        }
        return new EligibilityDecision(exercise.getId(), false, Set.copyOf(aggregateReasons));
    }
}

