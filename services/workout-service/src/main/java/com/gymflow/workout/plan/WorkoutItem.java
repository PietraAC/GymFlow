package com.gymflow.workout.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name = "workout_items")
public class WorkoutItem {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "day_id", nullable = false) private WorkoutDay day;
    @Column(name = "exercise_id", nullable = false) private UUID exerciseId;
    @Column(nullable = false) private int position;
    private Integer sets;
    @Column(name = "repetition_min") private Integer repetitionMin;
    @Column(name = "repetition_max") private Integer repetitionMax;
    @Column(name = "duration_seconds") private Integer durationSeconds;
    @Column(name = "rest_seconds") private Integer restSeconds;
    @Column(name = "optional_load_kg", precision = 7, scale = 2) private BigDecimal optionalLoadKg;
    @Column(length = 500) private String notes;
    protected WorkoutItem() {}
    public WorkoutItem(UUID id, UUID exerciseId, int position, Integer sets, Integer repetitionMin, Integer repetitionMax,
                       Integer durationSeconds, Integer restSeconds, BigDecimal load, String notes) {
        this.id = id == null ? UUID.randomUUID() : id; this.exerciseId = exerciseId; this.position = position; this.sets = sets;
        this.repetitionMin = repetitionMin; this.repetitionMax = repetitionMax; this.durationSeconds = durationSeconds;
        this.restSeconds = restSeconds; this.optionalLoadKg = load; this.notes = notes == null ? null : notes.trim();
    }
    void attach(WorkoutDay day) { this.day = day; }
    public UUID getId() { return id; } public UUID getExerciseId() { return exerciseId; } public int getPosition() { return position; }
    public Integer getSets() { return sets; } public Integer getRepetitionMin() { return repetitionMin; }
    public Integer getRepetitionMax() { return repetitionMax; } public Integer getDurationSeconds() { return durationSeconds; }
    public Integer getRestSeconds() { return restSeconds; } public BigDecimal getOptionalLoadKg() { return optionalLoadKg; }
    public String getNotes() { return notes; }
}
