package com.gymflow.workout.integration.assistant;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.slf4j.MDC;

@Configuration
public class AssistantClientConfiguration {
    @Bean RestClient assistantRestClient(@Value("${app.clients.assistant-service.base-url}") String url,
                                         @Value("${app.clients.assistant-service.connect-timeout}") Duration connect,
                                         @Value("${app.clients.assistant-service.read-timeout}") Duration read) {
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
