package com.gymflow.workout.profile;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {
    @Id @Column(name = "identity_subject", length = 100) private String identitySubject;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Goal goal;
    @Enumerated(EnumType.STRING) @Column(name = "experience_level", nullable = false, length = 30) private ExperienceLevel experienceLevel;
    @Column(name = "days_per_week", nullable = false) private int daysPerWeek;
    @Column(name = "session_duration_minutes", nullable = false) private int sessionDurationMinutes;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "student_profile_preferred_equipment", joinColumns = @JoinColumn(name = "identity_subject"))
    @Column(name = "equipment_type_id", nullable = false) private Set<UUID> preferredEquipmentTypeIds = new LinkedHashSet<>();
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected StudentProfile() {}
    public StudentProfile(String subject) { this.identitySubject = subject; }
    public void update(Goal goal, ExperienceLevel level, int days, int duration, Set<UUID> equipment) {
        this.goal = goal; this.experienceLevel = level; this.daysPerWeek = days; this.sessionDurationMinutes = duration;
        this.preferredEquipmentTypeIds.clear(); this.preferredEquipmentTypeIds.addAll(equipment == null ? Set.of() : equipment);
    }
    @PrePersist void create() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void updateTimestamp() { updatedAt = Instant.now(); }
    public String getIdentitySubject() { return identitySubject; }
    public Goal getGoal() { return goal; }
    public ExperienceLevel getExperienceLevel() { return experienceLevel; }
    public int getDaysPerWeek() { return daysPerWeek; }
    public int getSessionDurationMinutes() { return sessionDurationMinutes; }
    public Set<UUID> getPreferredEquipmentTypeIds() { return Set.copyOf(preferredEquipmentTypeIds); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
