package com.gymflow.gym.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymflow.gym.exercise.ExerciseRepository;
import com.gymflow.gym.gym.GymRepository;
import com.gymflow.gym.unit.GymUnitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.test.database.replace=none"
})
@Testcontainers(disabledWithoutDocker = true)
class GymPersistenceMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6-alpine3.24")
        .withDatabaseName("gym_test")
        .withUsername("gym_test")
        .withPassword("gym_test");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired GymRepository gyms;
    @Autowired GymUnitRepository units;
    @Autowired ExerciseRepository exercises;

    @Test
    void migrationsCreateSchemaAndReproducibleSeeds() {
        assertThat(gyms.count()).isEqualTo(2);
        assertThat(units.count()).isEqualTo(3);
        assertThat(exercises.count()).isEqualTo(22);
        assertThat(exercises.findByActiveTrueOrderByNameAsc()).allMatch(exercise -> !exercise.getName().isBlank());
    }
}
