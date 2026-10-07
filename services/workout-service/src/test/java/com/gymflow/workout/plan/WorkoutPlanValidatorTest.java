package com.gymflow.workout.plan;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gymflow.workout.shared.error.InvalidRequestException;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkoutPlanValidatorTest {
    private final WorkoutPlanValidator validator = new WorkoutPlanValidator();

    @Test
    void strengthRequiresSetsAndRepetitions() {
        PlanModels.ItemRequest valid = item(3, 8, 12, null, BigDecimal.TEN);
        assertThatCode(() -> validator.validateItem(valid, ExerciseKind.STRENGTH)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validateItem(item(null, null, null, 60, null), ExerciseKind.STRENGTH))
            .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void warmupAcceptsDurationWithoutStrengthFields() {
        assertThatCode(() -> validator.validateItem(item(null, null, null, 300, null), ExerciseKind.WARMUP))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validateItem(item(null, null, null, null, null), ExerciseKind.STRETCHING))
            .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsPartialRepetitionSchemeAndLoadOutsideStrength() {
        assertThatThrownBy(() -> validator.validateItem(item(3, 10, null, null, null), ExerciseKind.WARMUP))
            .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> validator.validateItem(item(null, null, null, 30, BigDecimal.ONE), ExerciseKind.STRETCHING))
            .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsDuplicateDayAndItemPositions() {
        PlanModels.ItemRequest first = item(3, 8, 12, null, null);
        PlanModels.ItemRequest second = new PlanModels.ItemRequest(null, UUID.randomUUID(), 1, 3, 8, 12, null, 60, null, null);
        PlanModels.DayRequest day = new PlanModels.DayRequest(null, 1, "Dia", List.of(first, second));
        assertThatThrownBy(() -> validator.validateStructure(List.of(day), false))
            .isInstanceOf(InvalidRequestException.class).hasMessageContaining("posições dos exercícios");
    }

    private PlanModels.ItemRequest item(Integer sets, Integer min, Integer max, Integer duration, BigDecimal load) {
        return new PlanModels.ItemRequest(null, UUID.randomUUID(), 1, sets, min, max, duration, 60, load, null);
    }
}
