package com.gymflow.workout.integration.gym;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.slf4j.MDC;

@Configuration
public class GymClientConfiguration {
    @Bean
    RestClient gymRestClient(@Value("${app.clients.gym-service.base-url}") String baseUrl,
                             @Value("${app.clients.gym-service.connect-timeout}") Duration connectTimeout,
                             @Value("${app.clients.gym-service.read-timeout}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).requestInterceptor((request, body, execution) -> {
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) request.getHeaders().set("X-Correlation-Id", correlationId);
            return execution.execute(request, body);
        }).build();
    }
}
