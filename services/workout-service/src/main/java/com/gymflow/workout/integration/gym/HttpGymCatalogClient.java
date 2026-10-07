package com.gymflow.workout.integration.gym;

import com.gymflow.workout.shared.error.DependencyUnavailableException;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpGymCatalogClient implements GymCatalogClient {
    private final RestClient client;
    public HttpGymCatalogClient(RestClient gymRestClient) { this.client = gymRestClient; }

    @Override
    public EligibilityResult validate(UUID unitId, Set<UUID> exerciseIds, String bearerToken) {
        try {
            EligibilityResult result = client.post().uri("/api/v1/units/{unitId}/exercise-eligibility", unitId)
                .headers(headers -> headers.setBearerAuth(bearerToken))
                .body(new EligibilityRequest(exerciseIds))
                .retrieve().body(EligibilityResult.class);
            if (result == null) throw new RestClientException("Resposta vazia do gym-service");
            return result;
        } catch (RestClientException exception) {
            throw new DependencyUnavailableException(
                "Não foi possível validar o catálogo no gym-service; a alteração não foi aplicada", exception);
        }
    }
    private record EligibilityRequest(Set<UUID> exerciseIds) {}
}
