package com.gymflow.gym.inventoryevent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class OutboxPublisherIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6-alpine3.24")
        .withDatabaseName("gym_outbox_test").withUsername("gym_test").withPassword("gym_test");
    @Container static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4.3.6-management-alpine");

    private CachingConnectionFactory connection;
    private JdbcTemplate jdbc;
    private OutboxRepository repository;
    private RabbitTemplate rabbit;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        repository = new OutboxRepository(jdbc, new ObjectMapper().findAndRegisterModules());

        connection = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getAmqpPort());
        connection.setUsername(RABBIT.getAdminUsername());
        connection.setPassword(RABBIT.getAdminPassword());
        connection.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connection.setPublisherReturns(true);
        RabbitAdmin admin = new RabbitAdmin(connection);
        TopicExchange exchange = new TopicExchange("gymflow.inventory.test", true, false);
        Queue queue = new Queue("gymflow.inventory.test.queue", true, false, false);
        admin.declareExchange(exchange);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("inventory.availability.changed.v1"));
        rabbit = new RabbitTemplate(connection);
    }

    @AfterEach void tearDown() { if (connection != null) connection.destroy(); }

    @Test
    void publishesPersistentEventOnlyAfterBrokerConfirmation() {
        EquipmentAvailabilityChanged event = EquipmentAvailabilityChanged.create(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 4, 7);
        repository.append(event, "inventory.availability.changed.v1");

        new OutboxPublisher(repository, rabbit, "gymflow.inventory.test", Duration.ofSeconds(5), 20).publishPending();

        assertThat(rabbit.receive("gymflow.inventory.test.queue", 5_000)).isNotNull();
        assertThat(jdbc.queryForObject("SELECT status FROM outbox_events WHERE event_id=?", String.class, event.eventId()))
            .isEqualTo("PUBLISHED");
    }
}
