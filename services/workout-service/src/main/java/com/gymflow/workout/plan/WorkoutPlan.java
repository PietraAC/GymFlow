package com.gymflow.workout.plan;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity @Table(name = "workout_plans")
public class WorkoutPlan {
    @Id private UUID id;
    @Column(name = "identity_subject", nullable = false, length = 100) private String identitySubject;
    @Column(name = "unit_id", nullable = false) private UUID unitId;
    @Column(nullable = false, length = 120) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PlanStatus status;
    @Version @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC") private List<WorkoutDay> days = new ArrayList<>();

    protected WorkoutPlan() {}
    public WorkoutPlan(String subject, UUID unitId, String name) {
        this.id = UUID.randomUUID(); this.identitySubject = subject; this.unitId = unitId; this.name = name.trim(); this.status = PlanStatus.DRAFT;
    }
    public void replace(UUID unitId, String name, List<WorkoutDay> newDays) {
        this.unitId = unitId; this.name = name.trim(); days.clear(); newDays.forEach(this::addDay);
    }
    private void addDay(WorkoutDay day) { day.attach(this); days.add(day); }
    public void activate() { status = PlanStatus.ACTIVE; }
    public void archive() { status = PlanStatus.ARCHIVED; }
    @PrePersist void create() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void updateTimestamp() { updatedAt = Instant.now(); }
    public UUID getId() { return id; } public String getIdentitySubject() { return identitySubject; }
    public UUID getUnitId() { return unitId; } public String getName() { return name; } public PlanStatus getStatus() { return status; }
    public long getVersion() { return version; } public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
    public List<WorkoutDay> getDays() { return List.copyOf(days); }
}
