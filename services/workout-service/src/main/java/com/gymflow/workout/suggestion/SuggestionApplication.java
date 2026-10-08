package com.gymflow.workout.suggestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "suggestion_applications")
public class SuggestionApplication {
    @Id private UUID id;
    @Column(name = "identity_subject", nullable = false, length = 100) private String identitySubject;
    @Column(name = "idempotency_key", nullable = false, length = 100) private String idempotencyKey;
    @Column(name = "suggestion_id", nullable = false) private UUID suggestionId;
    @Column(name = "plan_id", nullable = false) private UUID planId;
    @Column(name = "result_plan_version", nullable = false) private long resultPlanVersion;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected SuggestionApplication() {}
    public SuggestionApplication(String subject, String key, UUID suggestionId, UUID planId, long resultVersion) {
        this.id = UUID.randomUUID(); this.identitySubject = subject; this.idempotencyKey = key;
        this.suggestionId = suggestionId; this.planId = planId; this.resultPlanVersion = resultVersion;
    }
    @PrePersist void create() { createdAt = Instant.now(); }
    public String getIdentitySubject() { return identitySubject; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getSuggestionId() { return suggestionId; }
    public UUID getPlanId() { return planId; }
}
