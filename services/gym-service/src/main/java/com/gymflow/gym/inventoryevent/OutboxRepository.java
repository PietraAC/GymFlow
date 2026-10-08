package com.gymflow.gym.inventoryevent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public OutboxRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void append(EquipmentAvailabilityChanged event, String routingKey) {
        jdbc.update("""
            INSERT INTO outbox_events(id, event_id, aggregate_id, aggregate_version, event_type, routing_key,
              payload, status, attempts, next_attempt_at, created_at)
            VALUES (?,?,?,?,?,?,?::jsonb,'PENDING',0,?,?)
            """, UUID.randomUUID(), event.eventId(), event.equipmentTypeId(), event.aggregateVersion(),
            event.eventType(), routingKey, json(event), Timestamp.from(event.occurredAt()), Timestamp.from(event.occurredAt()));
    }

    public List<PendingEvent> pending(int limit) {
        return jdbc.query("""
            SELECT id, event_id, routing_key, payload::text, attempts
            FROM outbox_events
            WHERE status='PENDING' AND next_attempt_at <= now()
            ORDER BY created_at, id
            LIMIT ?
            """, (rs, row) -> new PendingEvent(rs.getObject("id", UUID.class),
            rs.getObject("event_id", UUID.class), rs.getString("routing_key"), rs.getString("payload"),
            rs.getInt("attempts")), limit);
    }

    public void markPublished(UUID id) {
        jdbc.update("""
            UPDATE outbox_events
            SET status='PUBLISHED', published_at=now(), last_error=NULL
            WHERE id=? AND status='PENDING'
            """, id);
    }

    public void markFailed(UUID id, int previousAttempts, String errorCategory) {
        int attempts = previousAttempts + 1;
        long delaySeconds = Math.min(60, 1L << Math.min(attempts, 6));
        jdbc.update("""
            UPDATE outbox_events
            SET attempts=?, next_attempt_at=?, last_error=?
            WHERE id=? AND status='PENDING'
            """, attempts, Timestamp.from(Instant.now().plusSeconds(delaySeconds)), errorCategory, id);
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Falha ao serializar evento de inventario", exception); }
    }

    public record PendingEvent(UUID id, UUID eventId, String routingKey, String payload, int attempts) {}
}
