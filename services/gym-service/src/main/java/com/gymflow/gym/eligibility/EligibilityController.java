package com.gymflow.gym.eligibility;

import com.gymflow.gym.exercise.Difficulty;
import com.gymflow.gym.exercise.ExerciseKind;
import com.gymflow.gym.exercise.ExerciseModels;
import com.gymflow.gym.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/units/{unitId}")
@SecurityRequirement(name = "bearerAuth")
public class EligibilityController {
    private final EligibilityService service;
    public EligibilityController(EligibilityService service) { this.service = service; }

    @GetMapping("/eligible-exercises")
    @Operation(summary = "Lista exercícios ativos cujas opções de requisitos são satisfeitas pela unidade")
    public PageResponse<ExerciseModels.ExerciseResponse> eligible(@PathVariable UUID unitId,
                                                                   @AuthenticationPrincipal Jwt jwt,
                                                                   @RequestParam(required = false) String muscleGroup,
                                                                   @RequestParam(required = false) ExerciseKind kind,
                                                                   @RequestParam(required = false) Difficulty difficulty,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return service.eligible(unitId, jwt.getSubject(), muscleGroup, kind, difficulty, page, size);
    }

    @PostMapping("/exercise-eligibility")
    @Operation(summary = "Valida em lote a elegibilidade atual de IDs do catálogo")
    public EligibilityModels.EligibilityResponse validate(@PathVariable UUID unitId, @AuthenticationPrincipal Jwt jwt,
                                                           @Valid @RequestBody EligibilityModels.EligibilityRequest request) {
        return service.validate(unitId, jwt.getSubject(), request.exerciseIds());
    }
}

