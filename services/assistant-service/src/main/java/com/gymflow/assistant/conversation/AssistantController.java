package com.gymflow.assistant.conversation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('STUDENT')")
@SecurityRequirement(name = "bearerAuth")
public class AssistantController {
    private final AssistantService service;
    public AssistantController(AssistantService service) { this.service = service; }

    @PostMapping("/conversations") @ResponseStatus(HttpStatus.CREATED)
    public AssistantModels.ConversationResponse create(@AuthenticationPrincipal Jwt jwt,
                                                        @Valid @RequestBody AssistantModels.CreateConversationRequest request) {
        return service.create(jwt.getSubject(), request.planId(), jwt.getTokenValue());
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<AssistantModels.MessageResponse> messages(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable UUID conversationId) {
        return service.messages(jwt.getSubject(), conversationId);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public AssistantModels.AssistantTurnResponse send(@AuthenticationPrincipal Jwt jwt,
                                                       @PathVariable UUID conversationId,
                                                      @Valid @RequestBody AssistantModels.SendMessageRequest request) {
        return service.send(jwt.getSubject(), conversationId, request.text(), jwt.getTokenValue());
    }

    @GetMapping("/suggestions/{suggestionId}")
    public AssistantModels.SuggestionResponse suggestion(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable UUID suggestionId) {
        return service.suggestion(jwt.getSubject(), suggestionId);
    }

    @PostMapping("/plans/{planId}/suggestions") @ResponseStatus(HttpStatus.CREATED)
    public AssistantModels.SuggestionResponse generateForPlan(@AuthenticationPrincipal Jwt jwt,
                                                               @PathVariable UUID planId) {
        return service.generateForPlan(jwt.getSubject(), planId, jwt.getTokenValue());
    }
}
