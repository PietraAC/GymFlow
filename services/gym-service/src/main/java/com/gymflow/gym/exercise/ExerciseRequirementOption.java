package com.gymflow.gym.exercise;

import com.gymflow.gym.equipment.EquipmentType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "exercise_requirement_options")
public class ExerciseRequirementOption {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    private int position;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "exercise_requirement_equipment",
        joinColumns = @JoinColumn(name = "requirement_option_id"),
        inverseJoinColumns = @JoinColumn(name = "equipment_type_id"))
    private Set<EquipmentType> requiredEquipmentTypes = new LinkedHashSet<>();

    protected ExerciseRequirementOption() {}

    public ExerciseRequirementOption(UUID id, int position, Set<EquipmentType> requiredEquipmentTypes) {
        this.id = id;
        this.position = position;
        this.requiredEquipmentTypes = new LinkedHashSet<>(requiredEquipmentTypes);
    }

    public UUID getId() { return id; }
    public int getPosition() { return position; }
    public Set<EquipmentType> getRequiredEquipmentTypes() { return Set.copyOf(requiredEquipmentTypes); }
}
