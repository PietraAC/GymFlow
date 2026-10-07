package com.gymflow.gym.exercise;

import com.gymflow.gym.shared.api.PageRequestFactory;
import com.gymflow.gym.shared.api.PageResponse;
import jakarta.persistence.criteria.JoinType;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciseCatalogService {
    private static final Set<String> SORTS = Set.of("name", "difficulty", "kind");
    private final ExerciseRepository exercises;

    public ExerciseCatalogService(ExerciseRepository exercises) { this.exercises = exercises; }

    @Transactional(readOnly = true)
    public PageResponse<ExerciseModels.ExerciseResponse> list(String muscleGroup, ExerciseKind kind, Difficulty difficulty,
                                                               int page, int size, String sort) {
        Specification<Exercise> spec = (root, query, cb) -> cb.isTrue(root.get("active"));
        if (kind != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("kind"), kind));
        if (difficulty != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("difficulty"), difficulty));
        if (muscleGroup != null && !muscleGroup.isBlank()) {
            String normalized = muscleGroup.trim().toUpperCase();
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                var primary = root.<Exercise, String>join("primaryMuscleGroups", JoinType.LEFT);
                var secondary = root.<Exercise, String>join("secondaryMuscleGroups", JoinType.LEFT);
                return cb.or(cb.equal(primary, normalized), cb.equal(secondary, normalized));
            });
        }
        return PageResponse.from(exercises.findAll(spec, PageRequestFactory.create(page, size, sort, SORTS)),
            ExerciseModels.ExerciseResponse::from);
    }
}

