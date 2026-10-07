package com.gymflow.workout.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymflow.workout.plan.WorkoutPlan;
import com.gymflow.workout.plan.WorkoutPlanRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.test.database.replace=none"})
@Testcontainers(disabledWithoutDocker = true)
class WorkoutPersistenceMigrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6-alpine3.24")
        .withDatabaseName("workout_test").withUsername("workout_test").withPassword("workout_test");

    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired WorkoutPlanRepository plans;
    @Test void migrationsCreateSchemaAcceptedByHibernate() {
        WorkoutPlan saved = plans.saveAndFlush(new WorkoutPlan("student-test", UUID.randomUUID(), "Plano teste"));
        assertThat(plans.findOneByIdAndIdentitySubject(saved.getId(), "student-test")).isPresent();
    }
}
