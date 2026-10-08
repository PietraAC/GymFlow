package com.gymflow.workout.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymflow.workout.plan.WorkoutPlan;
import com.gymflow.workout.plan.WorkoutPlanRepository;
import com.gymflow.workout.plan.WorkoutDay;
import com.gymflow.workout.inventoryevent.EquipmentAvailabilityChanged;
import com.gymflow.workout.inventoryevent.InventoryEventProcessor;
import com.gymflow.workout.inventoryevent.InventoryEventRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;
    @Test void migrationsCreateSchemaAcceptedByHibernate() {
        WorkoutPlan saved = plans.saveAndFlush(new WorkoutPlan("student-test", UUID.randomUUID(), "Plano teste"));
        assertThat(plans.findOneByIdAndIdentitySubject(saved.getId(), "student-test")).isPresent();
    }

    @Test void replacingOnlyChildrenAdvancesAggregateVersion() {
        WorkoutPlan saved = plans.saveAndFlush(new WorkoutPlan("student-test", UUID.randomUUID(), "Plano teste"));
        long originalVersion = saved.getVersion();

        saved.replace(saved.getUnitId(), saved.getName(), List.of(new WorkoutDay(null, 1, "Dia 1", List.of())));
        plans.saveAndFlush(saved);
        entityManager.clear();

        WorkoutPlan reloaded = plans.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getVersion()).isGreaterThan(originalVersion);
    }

    @Test void inventoryConsumerIsIdempotentAndIgnoresOlderAggregateVersions() {
        UUID unitId = UUID.randomUUID();
        UUID equipmentTypeId = UUID.randomUUID();
        WorkoutPlan saved = plans.saveAndFlush(new WorkoutPlan("student-test", unitId, "Plano teste"));
        InventoryEventProcessor processor = new InventoryEventProcessor(new InventoryEventRepository(jdbc));
        EquipmentAvailabilityChanged current = new EquipmentAvailabilityChanged(UUID.randomUUID(),
            EquipmentAvailabilityChanged.TYPE, 1, Instant.now(), UUID.randomUUID(), unitId, equipmentTypeId, 0, 3);
        EquipmentAvailabilityChanged older = new EquipmentAvailabilityChanged(UUID.randomUUID(),
            EquipmentAvailabilityChanged.TYPE, 1, Instant.now(), current.gymId(), unitId, equipmentTypeId, 2, 2);

        processor.process(current);
        processor.process(current);
        processor.process(older);
        entityManager.clear();

        assertThat(plans.findById(saved.getId()).orElseThrow().isInventoryRevalidationRequired()).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_inventory_events", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT aggregate_version FROM inventory_event_versions WHERE unit_id=? AND equipment_type_id=?",
            Long.class, unitId, equipmentTypeId)).isEqualTo(3L);
    }
}
