package com.gymflow.assistant.provider;

import com.gymflow.assistant.conversation.AssistantModels;
import com.gymflow.assistant.integration.AssistantContext;
import java.util.Comparator;
import java.util.ArrayList;
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

    @Override
    public CompletionResult complete(AssistantContext context) {
        List<AssistantContext.Exercise> candidates = context.eligibleExercises().stream()
            .sorted(Comparator.comparing((AssistantContext.Exercise exercise) ->
                    !preferred(exercise, context.profile().preferredEquipmentTypeIds()))
                .thenComparing(AssistantContext.Exercise::name).thenComparing(AssistantContext.Exercise::id))
            .toList();
        if (candidates.isEmpty()) {
            return new CompletionResult("Não encontrei exercícios elegíveis para montar o treino.", false,
                List.of("O catálogo elegível da unidade está vazio."),
                new AssistantModels.CompletionProposal(List.of(), List.of()));
        }
        List<AssistantModels.ExistingDayAddition> additions = new ArrayList<>();
        int cursor = 0;
        for (AssistantContext.Day day : context.plan().days()) {
            int missingItems = Math.max(0, 4 - day.items().size());
            if (missingItems == 0) continue;
            Set<UUID> existing = day.items().stream().map(AssistantContext.Item::exerciseId).collect(java.util.stream.Collectors.toSet());
            List<AssistantModels.SuggestedItem> items = itemsForDay(candidates, existing,
                day.items().size() + 1, cursor, missingItems);
            cursor += items.size();
            additions.add(new AssistantModels.ExistingDayAddition(day.id(), items));
        }
        List<AssistantModels.SuggestedDay> newDays = new ArrayList<>();
        for (int position = context.plan().days().size() + 1;
             position <= context.plan().targetDaysPerWeek(); position++) {
            List<AssistantModels.SuggestedItem> items = itemsForDay(candidates, Set.of(), 1, cursor, 4);
            cursor += items.size();
            newDays.add(new AssistantModels.SuggestedDay(position, "Dia " + position, items));
        }
        return new CompletionResult("Montei a estrutura completa do treino com base no objetivo, na frequência semanal e no catálogo da unidade.",
            false, List.of("Revise os exercícios e ajuste cargas manualmente antes de ativar o plano.",
                "A proposta preserva tudo o que já estava no rascunho."),
            new AssistantModels.CompletionProposal(newDays, additions));
    }

    private List<AssistantModels.SuggestedItem> itemsForDay(List<AssistantContext.Exercise> candidates,
                                                             Set<UUID> existing, int firstPosition, int cursor,
                                                             int requested) {
        List<AssistantModels.SuggestedItem> result = new ArrayList<>();
        Set<UUID> used = new HashSet<>(existing);
        int desired = Math.min(requested, Math.max(0, candidates.size() - existing.size()));
        for (int offset = 0; result.size() < desired && offset < candidates.size() * 2; offset++) {
            AssistantContext.Exercise exercise = candidates.get((cursor + offset) % candidates.size());
            if (!used.add(exercise.id())) continue;
            boolean strength = "STRENGTH".equals(exercise.kind());
            result.add(new AssistantModels.SuggestedItem(exercise.id(), firstPosition + result.size(),
                strength ? 3 : null, strength ? 8 : null, strength ? 12 : null,
                strength ? null : 60, 60,
                "Exercício elegível selecionado para compor uma sessão equilibrada no modo demo."));
        }
        return List.copyOf(result);
    }

    private boolean preferred(AssistantContext.Exercise exercise, Set<UUID> preferences) {
        if (preferences == null || preferences.isEmpty()) return false;
        return exercise.requirementOptions() != null && exercise.requirementOptions().stream()
            .flatMap(option -> option.equipmentTypeIds().stream()).anyMatch(preferences::contains);
    }

    @Override public AssistantModels.Source source() { return AssistantModels.Source.DEMO; }
}
