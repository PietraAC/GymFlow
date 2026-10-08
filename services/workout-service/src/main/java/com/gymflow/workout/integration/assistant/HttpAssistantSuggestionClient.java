package com.gymflow.workout.integration.assistant;

import com.gymflow.workout.shared.error.ConflictException;
import com.gymflow.workout.shared.error.DependencyUnavailableException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpAssistantSuggestionClient implements AssistantSuggestionClient {
    private final RestClient client;
    public HttpAssistantSuggestionClient(RestClient assistantRestClient) { this.client = assistantRestClient; }
    @Override public Suggestion get(UUID id, String bearerToken) {
        try {
            Suggestion result = client.get().uri("/api/v1/suggestions/{id}", id)
                .headers(headers -> headers.setBearerAuth(bearerToken)).retrieve().body(Suggestion.class);
            if (result == null) throw new RestClientException("Resposta vazia do assistant-service");
            return result;
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ConflictException("A sugestão não existe ou não pertence ao aluno autenticado");
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) throw new ConflictException("A sugestão não pode mais ser aplicada");
            throw new DependencyUnavailableException("O assistant-service está temporariamente indisponível", exception);
        } catch (RestClientException exception) {
            throw new DependencyUnavailableException("O assistant-service está temporariamente indisponível", exception);
        }
    }
}
