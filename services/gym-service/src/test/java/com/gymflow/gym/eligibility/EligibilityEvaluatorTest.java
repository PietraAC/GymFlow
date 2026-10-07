package com.gymflow.gym.eligibility;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymflow.gym.equipment.EquipmentType;
import com.gymflow.gym.equipment.UnitEquipment;
import com.gymflow.gym.exercise.Difficulty;
import com.gymflow.gym.exercise.Exercise;
import com.gymflow.gym.exercise.ExerciseKind;
import com.gymflow.gym.exercise.ExerciseRequirementOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EligibilityEvaluatorTest {
    private final EligibilityEvaluator evaluator = new EligibilityEvaluator();
    private final EquipmentType dumbbells = type("Halteres", true);
    private final EquipmentType bench = type("Banco", true);

    @Test
    void cumulativeRequirementsNeedEveryEquipmentInOneOption() {
        Exercise exercise = exercise();
        exercise.addRequirementOption(option(dumbbells, bench));

        EligibilityDecision missingBench = evaluator.evaluate(exercise,
            Map.of(dumbbells.getId(), available(dumbbells, 2)));
        EligibilityDecision complete = evaluator.evaluate(exercise,
            Map.of(dumbbells.getId(), available(dumbbells, 2), bench.getId(), available(bench, 1)));

        assertThat(missingBench.eligible()).isFalse();
        assertThat(missingBench.reasons()).contains(EligibilityReason.MISSING_EQUIPMENT);
        assertThat(complete.eligible()).isTrue();
    }

    @Test
    void alternativeOptionIsEnough() {
        Exercise exercise = exercise();
        exercise.addRequirementOption(option(dumbbells, bench));
        exercise.addRequirementOption(option(dumbbells));

        assertThat(evaluator.evaluate(exercise, Map.of(dumbbells.getId(), available(dumbbells, 1))).eligible()).isTrue();
    }

    @Test
    void emptyRequirementOptionRepresentsBodyweight() {
        Exercise exercise = exercise();
        exercise.addRequirementOption(option());

        assertThat(evaluator.evaluate(exercise, Map.of()).eligible()).isTrue();
    }

    @Test
    void zeroAvailableQuantityDoesNotSatisfyRequirement() {
        Exercise exercise = exercise();
        exercise.addRequirementOption(option(dumbbells));

        EligibilityDecision result = evaluator.evaluate(exercise, Map.of(dumbbells.getId(), available(dumbbells, 0)));

        assertThat(result.eligible()).isFalse();
        assertThat(result.reasons()).containsExactly(EligibilityReason.EQUIPMENT_UNAVAILABLE);
    }

    @Test
    void inactiveEquipmentTypeDoesNotSatisfyRequirement() {
        EquipmentType inactive = type("Equipamento desativado", false);
        Exercise exercise = exercise();
        exercise.addRequirementOption(option(inactive));

        EligibilityDecision result = evaluator.evaluate(exercise, Map.of(inactive.getId(), available(inactive, 1)));

        assertThat(result.eligible()).isFalse();
        assertThat(result.reasons()).containsExactly(EligibilityReason.EQUIPMENT_TYPE_INACTIVE);
    }

    private Exercise exercise() {
        return new Exercise(UUID.randomUUID(), "Exercício", ExerciseKind.STRENGTH, "PADRAO", Difficulty.BEGINNER, true);
    }

    private ExerciseRequirementOption option(EquipmentType... types) {
        return new ExerciseRequirementOption(UUID.randomUUID(), 0, Set.of(types));
    }

    private EquipmentType type(String name, boolean active) {
        return new EquipmentType(UUID.randomUUID(), name, "Descrição", active);
    }

    private UnitEquipment available(EquipmentType type, int quantity) {
        return new UnitEquipment(UUID.randomUUID(), type.getId(), quantity, quantity, null);
    }
}

