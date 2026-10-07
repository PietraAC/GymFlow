package com.gymflow.gym.exercise;

import com.gymflow.gym.shared.persistence.AuditedEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "exercises")
public class Exercise extends AuditedEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 140)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExerciseKind kind;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "exercise_primary_muscle_groups", joinColumns = @JoinColumn(name = "exercise_id"))
    @Column(name = "muscle_group", nullable = false, length = 60)
    private Set<String> primaryMuscleGroups = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "exercise_secondary_muscle_groups", joinColumns = @JoinColumn(name = "exercise_id"))
    @Column(name = "muscle_group", nullable = false, length = 60)
    private Set<String> secondaryMuscleGroups = new LinkedHashSet<>();

    @Column(name = "movement_pattern", nullable = false, length = 80)
    private String movementPattern;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(nullable = false, length = 1500)
    private String instructions;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "exercise", fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private Set<ExerciseRequirementOption> requirementOptions = new LinkedHashSet<>();

    protected Exercise() {}

    public Exercise(UUID id, String name, ExerciseKind kind, String movementPattern, Difficulty difficulty, boolean active) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.movementPattern = movementPattern;
        this.difficulty = difficulty;
        this.instructions = "Siga a execução descrita no catálogo e ajuste o treino com orientação profissional quando necessário.";
        this.active = active;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public ExerciseKind getKind() { return kind; }
    public Set<String> getPrimaryMuscleGroups() { return Set.copyOf(primaryMuscleGroups); }
    public Set<String> getSecondaryMuscleGroups() { return Set.copyOf(secondaryMuscleGroups); }
    public String getMovementPattern() { return movementPattern; }
    public Difficulty getDifficulty() { return difficulty; }
    public String getInstructions() { return instructions; }
    public boolean isActive() { return active; }
    public Set<ExerciseRequirementOption> getRequirementOptions() { return Set.copyOf(requirementOptions); }

    public void addRequirementOption(ExerciseRequirementOption option) {
        requirementOptions.add(option);
    }
}
