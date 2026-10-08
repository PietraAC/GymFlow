package com.gymflow.assistant.provider;

import com.gymflow.assistant.conversation.AssistantModels;
import com.gymflow.assistant.integration.AssistantContext;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DemoTrainingAssistantProvider implements TrainingAssistantProvider {
    private static final List<String> PROFESSIONAL_TERMS = List.of(
        "lesão", "lesao", "dor", "diagnóstico", "diagnostico", "reabilitação", "reabilitacao", "fisioterapia"
    );

    @Override
    public ProviderResult generate(AssistantContext context, String userMessage) {
        String normalized = userMessage.toLowerCase(Locale.ROOT);
        if (PROFESSIONAL_TERMS.stream().anyMatch(normalized::contains)) {
            return new ProviderResult(
                "Não posso orientar tratamento, diagnóstico ou reabilitação. Procure um profissional habilitado para avaliar esse caso.",
                true, List.of("Nenhuma mudança foi proposta porque a pergunta exige orientação profissional."), List.of());
        }
        if (context.plan().days().isEmpty()) {
            return new ProviderResult("Crie pelo menos um dia no rascunho para que eu possa sugerir um exercício.", false,
                List.of("O modo demo não cria dias automaticamente."), List.of());
        }
        Set<UUID> current = new HashSet<>();
        context.plan().days().forEach(day -> day.items().forEach(item -> current.add(item.exerciseId())));
        AssistantContext.Exercise candidate = context.eligibleExercises().stream()
            .filter(exercise -> !current.contains(exercise.id()))
            .sorted(Comparator.comparing((AssistantContext.Exercise exercise) -> !preferred(exercise, context.profile().preferredEquipmentTypeIds()))
                .thenComparing(AssistantContext.Exercise::name).thenComparing(AssistantContext.Exercise::id))
            .findFirst().orElse(null);
        if (candidate == null) {
            return new ProviderResult("Não encontrei outro exercício elegível para acrescentar ao rascunho atual.", false,
                List.of("Todos os candidatos recebidos já estão no plano ou o catálogo elegível está vazio."), List.of());
        }
        AssistantContext.Day day = context.plan().days().stream().min(Comparator.comparingInt(AssistantContext.Day::position)).orElseThrow();
        int position = day.items().size() + 1;
        boolean strength = "STRENGTH".equals(candidate.kind());
        AssistantModels.SuggestionChange change = new AssistantModels.SuggestionChange(AssistantModels.Operation.ADD,
            day.id(), null, candidate.id(), position, strength ? 3 : null, strength ? 8 : null,
            strength ? 12 : null, strength ? null : 60, 60,
            "Complemento determinístico do modo demo usando apenas o catálogo elegível da unidade.");
        return new ProviderResult("Preparei uma sugestão estruturada com base no rascunho e no catálogo elegível. Nada foi alterado automaticamente pelo provedor.", false,
            List.of("A sugestão considera o plano salvo e o catálogo elegível atual.",
                preferred(candidate, context.profile().preferredEquipmentTypeIds())
                    ? "O candidato também corresponde a uma preferência de equipamento declarada."
                    : "Preferências são tratadas como preferência, não como restrição obrigatória."),
            List.of(change));
    }

    private boolean preferred(AssistantContext.Exercise exercise, Set<UUID> preferences) {
        if (preferences == null || preferences.isEmpty()) return false;
        return exercise.requirementOptions() != null && exercise.requirementOptions().stream()
            .flatMap(option -> option.equipmentTypeIds().stream()).anyMatch(preferences::contains);
    }

    @Override public AssistantModels.Source source() { return AssistantModels.Source.DEMO; }
}
