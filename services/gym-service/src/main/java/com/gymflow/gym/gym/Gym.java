package com.gymflow.gym.gym;

import com.gymflow.gym.shared.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "gyms")
public class Gym extends AuditedEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private boolean active;

    protected Gym() {}

    public Gym(String name, boolean active) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.active = active;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}

