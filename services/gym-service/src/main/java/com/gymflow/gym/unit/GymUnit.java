package com.gymflow.gym.unit;

import com.gymflow.gym.shared.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "gym_units")
public class GymUnit extends AuditedEntity {
    @Id
    private UUID id;

    @Column(name = "gym_id", nullable = false)
    private UUID gymId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false)
    private boolean active;

    protected GymUnit() {}

    public GymUnit(UUID gymId, String name, String city, boolean active) {
        this.id = UUID.randomUUID();
        this.gymId = gymId;
        this.name = name;
        this.city = city;
        this.active = active;
    }

    public void update(String name, String city, Boolean active) {
        if (name != null) this.name = name;
        if (city != null) this.city = city;
        if (active != null) this.active = active;
    }

    public UUID getId() { return id; }
    public UUID getGymId() { return gymId; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public boolean isActive() { return active; }
}

