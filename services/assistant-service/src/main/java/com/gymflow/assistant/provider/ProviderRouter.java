package com.gymflow.assistant.provider;

import com.gymflow.assistant.integration.AssistantContext;
import com.gymflow.assistant.shared.error.ProviderUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProviderRouter {
    private final DemoTrainingAssistantProvider demo;
    private final GeminiTrainingAssistantProvider gemini;
    private final String configured;

    public ProviderRouter(DemoTrainingAssistantProvider demo, GeminiTrainingAssistantProvider gemini,
                          @Value("${app.ai.provider}") String configured) {
        this.demo = demo; this.gemini = gemini; this.configured = configured;
    }

    public TrainingAssistantProvider selected() {
        return switch (configured.toLowerCase()) {
            case "demo" -> demo;
            case "gemini" -> gemini;
            default -> throw new ProviderUnavailableException("AI_PROVIDER deve ser demo ou gemini");
        };
    }
}
