package com.gymflow.gym.gym;

import com.gymflow.gym.shared.persistence.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "gym_admin_memberships")
public class GymAdminMembership extends AuditedEntity {
    @Id
    private UUID id;

    @Column(name = "gym_id", nullable = false)
    private UUID gymId;

    @Column(name = "identity_subject", nullable = false, length = 120)
    private String identitySubject;

    protected GymAdminMembership() {}

    public GymAdminMembership(UUID gymId, String identitySubject) {
        this.id = UUID.randomUUID();
        this.gymId = gymId;
        this.identitySubject = identitySubject;
    }

    public UUID getGymId() { return gymId; }
    public String getIdentitySubject() { return identitySubject; }
}

