package com.gymflow.assistant.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.assistant.conversation.AssistantModels;
import com.gymflow.assistant.shared.error.DependencyUnavailableException;
import com.gymflow.assistant.shared.error.InvalidRequestException;
import com.gymflow.assistant.shared.error.ResourceNotFoundException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class ContextService {
    private final RestClient workout;
    private final RestClient gym;
    private final ObjectMapper mapper;
    private final int candidateLimit;

    public ContextService(@Qualifier("assistantWorkoutRestClient") RestClient workout,
                          @Qualifier("assistantGymRestClient") RestClient gym, ObjectMapper mapper,
                          @Value("${app.ai.candidate-limit}") int candidateLimit) {
        this.workout = workout;
        this.gym = gym;
        this.mapper = mapper;
        this.candidateLimit = candidateLimit;
    }

    public AssistantContext build(UUID planId, String token, List<AssistantModels.MessageResponse> history) {
        try {
            AssistantContext.Profile profile = workout.get().uri("/api/v1/me/profile")
                .headers(h -> h.setBearerAuth(token)).retrieve().body(AssistantContext.Profile.class);
            AssistantContext.Plan plan = workout.get().uri("/api/v1/me/plans/{id}", planId)
                .headers(h -> h.setBearerAuth(token)).retrieve().body(AssistantContext.Plan.class);
            if (profile == null || plan == null) throw new RestClientException("Resposta vazia do workout-service");
            if (!"DRAFT".equals(plan.status())) throw new InvalidRequestException("Somente planos em rascunho podem receber sugestões aplicáveis");
            AssistantContext.Page<AssistantContext.Exercise> page = gym.get()
                .uri(uri -> uri.path("/api/v1/units/{id}/eligible-exercises").queryParam("size", 100).build(plan.unitId()))
                .headers(h -> h.setBearerAuth(token)).retrieve()
                .body(new ParameterizedTypeReference<AssistantContext.Page<AssistantContext.Exercise>>() {});
            if (page == null) throw new RestClientException("Resposta vazia do gym-service");
            List<AssistantContext.Exercise> exercises = page.content().stream()
                .sorted(Comparator.comparing(AssistantContext.Exercise::name).thenComparing(AssistantContext.Exercise::id))
                .limit(candidateLimit).toList();
            String fingerprint = fingerprint(profile, plan, exercises);
            return new AssistantContext(profile, plan, exercises, history, fingerprint);
        } catch (InvalidRequestException exception) {
            throw exception;
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound exception) {
            throw new ResourceNotFoundException("Perfil ou plano não encontrado");
        } catch (RestClientException exception) {
            throw new DependencyUnavailableException("Não foi possível montar o contexto do assistente", exception);
        }
    }

    public void requireOwnedPlan(UUID planId, String token) {
        try {
            workout.get().uri("/api/v1/me/plans/{id}", planId).headers(h -> h.setBearerAuth(token))
                .retrieve().onStatus(HttpStatusCode::isError, (request, response) -> {
                    if (response.getStatusCode().value() == 404) throw new ResourceNotFoundException("Plano não encontrado");
                }).toBodilessEntity();
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new DependencyUnavailableException("Não foi possível validar o plano no workout-service", exception);
        }
    }

    private String fingerprint(Object... values) {
        try {
            byte[] source = mapper.writeValueAsBytes(values);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Não foi possível calcular o fingerprint do contexto", exception);
        }
    }
}
