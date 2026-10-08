package com.gymflow.workout.suggestion;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuggestionApplicationRepository extends JpaRepository<SuggestionApplication, UUID> {
    Optional<SuggestionApplication> findByIdentitySubjectAndIdempotencyKey(String subject, String key);
    boolean existsBySuggestionId(UUID suggestionId);
}
