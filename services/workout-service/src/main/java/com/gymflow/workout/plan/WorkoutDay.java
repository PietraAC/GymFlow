package com.gymflow.workout.plan;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity @Table(name = "workout_days")
public class WorkoutDay {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "plan_id", nullable = false) private WorkoutPlan plan;
    @Column(nullable = false) private int position;
    @Column(nullable = false, length = 80) private String name;
    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC") private List<WorkoutItem> items = new ArrayList<>();
    protected WorkoutDay() {}
    public WorkoutDay(UUID id, int position, String name, List<WorkoutItem> items) {
        this.id = id == null ? UUID.randomUUID() : id; this.position = position; this.name = name.trim(); items.forEach(this::addItem);
    }
    void attach(WorkoutPlan plan) { this.plan = plan; }
    private void addItem(WorkoutItem item) { item.attach(this); items.add(item); }
    public UUID getId() { return id; } public int getPosition() { return position; } public String getName() { return name; }
    public List<WorkoutItem> getItems() { return List.copyOf(items); }
}
