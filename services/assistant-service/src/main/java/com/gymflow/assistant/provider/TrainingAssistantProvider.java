package com.gymflow.assistant.provider;

import com.gymflow.assistant.conversation.AssistantModels;
import com.gymflow.assistant.integration.AssistantContext;
import java.util.List;

public interface TrainingAssistantProvider {
    ProviderResult generate(AssistantContext context, String userMessage);
    AssistantModels.Source source();

    record ProviderResult(String reply, boolean needsProfessionalGuidance, List<String> observations,
                          List<AssistantModels.SuggestionChange> changes) {}
}
