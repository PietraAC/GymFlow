package com.gymflow.gym.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

class JwtValidationTest {
    @Test
    void rejectsUnexpectedAudience() {
        Jwt jwt = jwt(List.of("another-service"), Instant.now().minusSeconds(10), Instant.now().plusSeconds(60));
        assertThat(new JwtAudienceValidator("gym-service").validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void acceptsExpectedAudience() {
        Jwt jwt = jwt(List.of("gym-service"), Instant.now().minusSeconds(10), Instant.now().plusSeconds(60));
        assertThat(new JwtAudienceValidator("gym-service").validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void rejectsExpiredToken() {
        Jwt jwt = jwt(List.of("gym-service"), Instant.now().minusSeconds(120), Instant.now().minusSeconds(90));
        assertThat(new JwtTimestampValidator().validate(jwt).hasErrors()).isTrue();
    }

    private Jwt jwt(List<String> audiences, Instant issuedAt, Instant expiresAt) {
        return new Jwt("token", issuedAt, expiresAt, Map.of("alg", "none"),
            Map.of("sub", "subject", "aud", audiences));
    }
}

