package com.gymflow.assistant.conversation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.assistant.shared.error.ResourceNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AssistantRepository {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
    private static final TypeReference<List<AssistantModels.SuggestionChange>> CHANGE_LIST = new TypeReference<>() {};
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public AssistantRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public AssistantModels.ConversationResponse createConversation(String subject, UUID planId) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("INSERT INTO conversations(id, identity_subject, plan_id, created_at) VALUES (?,?,?,?)",
            id, subject, planId, Timestamp.from(now));
        return new AssistantModels.ConversationResponse(id, planId, now);
    }

    public AssistantModels.ConversationResponse ownedConversation(String subject, UUID id) {
        return jdbc.query("SELECT id, plan_id, created_at FROM conversations WHERE id=? AND identity_subject=?",
                (rs, row) -> new AssistantModels.ConversationResponse(rs.getObject("id", UUID.class),
                    rs.getObject("plan_id", UUID.class), rs.getTimestamp("created_at").toInstant()), id, subject)
            .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada"));
    }

    public AssistantModels.MessageResponse addMessage(UUID conversationId, String role, String text) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("INSERT INTO messages(id, conversation_id, role, text, created_at) VALUES (?,?,?,?,?)",
            id, conversationId, role, text, Timestamp.from(now));
        return new AssistantModels.MessageResponse(id, role, text, now);
    }

    public List<AssistantModels.MessageResponse> messages(UUID conversationId) {
        return jdbc.query("SELECT id, role, text, created_at FROM messages WHERE conversation_id=? ORDER BY created_at, id",
            (rs, row) -> new AssistantModels.MessageResponse(rs.getObject("id", UUID.class), rs.getString("role"),
                rs.getString("text"), rs.getTimestamp("created_at").toInstant()), conversationId);
    }

    public AssistantModels.SuggestionResponse saveSuggestion(UUID conversationId, String subject, UUID planId,
                                                               long planVersion, String fingerprint,
                                                               AssistantModels.Source source, String explanation,
                                                               List<String> observations,
                                                               List<AssistantModels.SuggestionChange> changes,
                                                               Instant expiresAt) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
            INSERT INTO suggestions(id, conversation_id, identity_subject, plan_id, base_plan_version,
              context_fingerprint, status, source, explanation, observations, proposed_changes, created_at, expires_at)
            VALUES (?,?,?,?,?,?,'AVAILABLE',?,?,?::jsonb,?::jsonb,?,?)
            """, id, conversationId, subject, planId, planVersion, fingerprint, source.name(), explanation,
            json(observations), json(changes), Timestamp.from(now), Timestamp.from(expiresAt));
        return new AssistantModels.SuggestionResponse(id, planId, planVersion, fingerprint, "AVAILABLE", source,
            explanation, observations, changes, now, expiresAt);
    }

    public AssistantModels.SuggestionResponse saveCompletionSuggestion(UUID conversationId, String subject, UUID planId,
                                                                         long planVersion, String fingerprint,
                                                                         AssistantModels.Source source, String explanation,
                                                                         List<String> observations,
                                                                         AssistantModels.CompletionProposal completion,
                                                                         Instant expiresAt) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
            INSERT INTO suggestions(id, conversation_id, identity_subject, plan_id, base_plan_version,
              context_fingerprint, status, source, suggestion_kind, explanation, observations, proposed_changes,
              completion_payload, created_at, expires_at)
            VALUES (?,?,?,?,?,?,'AVAILABLE',?,'WORKOUT_COMPLETION',?,?::jsonb,'[]'::jsonb,?::jsonb,?,?)
            """, id, conversationId, subject, planId, planVersion, fingerprint, source.name(), explanation,
            json(observations), json(completion), Timestamp.from(now), Timestamp.from(expiresAt));
        return new AssistantModels.SuggestionResponse(id, planId, planVersion, fingerprint, "AVAILABLE", source,
            AssistantModels.SuggestionKind.WORKOUT_COMPLETION, explanation, observations, List.of(), completion,
            now, expiresAt);
    }

    public AssistantModels.SuggestionResponse ownedSuggestion(String subject, UUID id) {
        return jdbc.query("SELECT * FROM suggestions WHERE id=? AND identity_subject=?", this::suggestion, id, subject)
            .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Sugestão não encontrada"));
    }

    private AssistantModels.SuggestionResponse suggestion(ResultSet rs, int row) throws SQLException {
        Instant expiresAt = rs.getTimestamp("expires_at").toInstant();
        String status = expiresAt.isBefore(Instant.now()) ? "EXPIRED" : rs.getString("status");
        AssistantModels.SuggestionKind kind = AssistantModels.SuggestionKind.valueOf(rs.getString("suggestion_kind"));
        AssistantModels.CompletionProposal completion = rs.getString("completion_payload") == null ? null
            : read(rs.getString("completion_payload"), new TypeReference<>() {});
        return new AssistantModels.SuggestionResponse(rs.getObject("id", UUID.class),
            rs.getObject("plan_id", UUID.class), rs.getLong("base_plan_version"),
            rs.getString("context_fingerprint"), status, AssistantModels.Source.valueOf(rs.getString("source")), kind,
            rs.getString("explanation"), read(rs.getString("observations"), STRING_LIST),
            read(rs.getString("proposed_changes"), CHANGE_LIST), completion,
            rs.getTimestamp("created_at").toInstant(), expiresAt);
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Falha ao serializar sugestão", exception); }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try { return mapper.readValue(value, type); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Sugestão persistida inválida", exception); }
    }
}
