package com.gymflow.workout.plan;

import com.gymflow.workout.profile.Goal;
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
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Goal goal;
    @Column(name = "target_days_per_week", nullable = false) private int targetDaysPerWeek;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PlanStatus status;
    @Version @Column(nullable = false) private long version;
    @Column(name = "inventory_revalidation_required", nullable = false) private boolean inventoryRevalidationRequired;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC") private List<WorkoutDay> days = new ArrayList<>();

    protected WorkoutPlan() {}
    public WorkoutPlan(String subject, UUID unitId, String name, Goal goal, int targetDaysPerWeek) {
        this.id = UUID.randomUUID(); this.identitySubject = subject; this.unitId = unitId; this.name = name.trim();
        this.goal = goal; this.targetDaysPerWeek = targetDaysPerWeek; this.status = PlanStatus.DRAFT;
    }
    public WorkoutPlan(String subject, UUID unitId, String name) {
        this(subject, unitId, name, Goal.GENERAL_FITNESS, 3);
    }
    public void replace(UUID unitId, String name, Goal goal, int targetDaysPerWeek, List<WorkoutDay> newDays) {
        this.unitId = unitId; this.name = name.trim(); this.goal = goal; this.targetDaysPerWeek = targetDaysPerWeek;
        days.clear(); newDays.forEach(this::addDay);
        // Changes to the inverse child collection alone do not necessarily dirty the owner.
        // Touching the aggregate root guarantees that its optimistic-lock version advances.
        this.updatedAt = Instant.now();
    }
    public void replace(UUID unitId, String name, List<WorkoutDay> newDays) {
        replace(unitId, name, goal, targetDaysPerWeek, newDays);
    }
    private void addDay(WorkoutDay day) { day.attach(this); days.add(day); }
    public void activate() { status = PlanStatus.ACTIVE; }
    public void archive() { status = PlanStatus.ARCHIVED; }
    public void confirmInventoryRevalidation() { inventoryRevalidationRequired = false; }
    @PrePersist void create() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void updateTimestamp() { updatedAt = Instant.now(); }
    public UUID getId() { return id; } public String getIdentitySubject() { return identitySubject; }
    public UUID getUnitId() { return unitId; } public String getName() { return name; } public PlanStatus getStatus() { return status; }
    public Goal getGoal() { return goal; } public int getTargetDaysPerWeek() { return targetDaysPerWeek; }
    public long getVersion() { return version; } public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
    public boolean isInventoryRevalidationRequired() { return inventoryRevalidationRequired; }
    public List<WorkoutDay> getDays() { return List.copyOf(days); }
}
