package com.gymflow.gym.exercise;

import com.gymflow.gym.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exercises")
@SecurityRequirement(name = "bearerAuth")
public class ExerciseController {
    private final ExerciseCatalogService service;
    public ExerciseController(ExerciseCatalogService service) { this.service = service; }

    @GetMapping
    public PageResponse<ExerciseModels.ExerciseResponse> list(@RequestParam(required = false) String muscleGroup,
                                                               @RequestParam(required = false) ExerciseKind kind,
                                                               @RequestParam(required = false) Difficulty difficulty,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size,
                                                               @RequestParam(defaultValue = "name,asc") String sort) {
        return service.list(muscleGroup, kind, difficulty, page, size, sort);
    }
}

