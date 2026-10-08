package com.gymflow.assistant.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.assistant.conversation.AssistantRepository;
import com.gymflow.assistant.shared.error.ResourceNotFoundException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class AssistantMigrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6-alpine3.24")
        .withDatabaseName("assistant_test").withUsername("assistant_test").withPassword("assistant_test");

    @Test void migrationsCreateConversationAndSuggestionSchema() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).load().migrate();
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var repository = new AssistantRepository(new JdbcTemplate(dataSource), new ObjectMapper());
        var conversation = repository.createConversation("student-test", UUID.randomUUID());
        var persisted = repository.ownedConversation("student-test", conversation.id());
        assertThat(persisted.id()).isEqualTo(conversation.id());
        assertThat(persisted.planId()).isEqualTo(conversation.planId());
        assertThat(persisted.createdAt()).isCloseTo(conversation.createdAt(),
            org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.MILLIS));
        assertThatThrownBy(() -> repository.ownedConversation("another-student", conversation.id()))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
