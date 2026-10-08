package com.gymflow.assistant.integration;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.slf4j.MDC;

@Configuration
public class ServiceClientsConfiguration {
    @Bean("assistantWorkoutRestClient")
    RestClient workout(@Value("${app.clients.workout-service.base-url}") String url,
                       @Value("${app.clients.workout-service.connect-timeout}") Duration connect,
                       @Value("${app.clients.workout-service.read-timeout}") Duration read) {
        return client(url, connect, read);
    }

    @Bean("assistantGymRestClient")
    RestClient gym(@Value("${app.clients.gym-service.base-url}") String url,
                   @Value("${app.clients.gym-service.connect-timeout}") Duration connect,
                   @Value("${app.clients.gym-service.read-timeout}") Duration read) {
        return client(url, connect, read);
    }

    private RestClient client(String url, Duration connect, Duration read) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(connect).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(read);
        return RestClient.builder().baseUrl(url).requestFactory(factory).requestInterceptor((request, body, execution) -> {
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) request.getHeaders().set("X-Correlation-Id", correlationId);
            return execution.execute(request, body);
        }).build();
    }
}
