package com.gymflow.workout.plan;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/me/plans") @PreAuthorize("hasRole('STUDENT')") @SecurityRequirement(name = "bearerAuth")
public class WorkoutPlanController {
    private final WorkoutPlanService service;
    public WorkoutPlanController(WorkoutPlanService service) { this.service = service; }

    @GetMapping public List<PlanModels.PlanSummary> list(@AuthenticationPrincipal Jwt jwt) { return service.list(jwt.getSubject()); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public PlanModels.PlanResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlanModels.CreatePlanRequest request) {
        return service.create(jwt.getSubject(), request);
    }
    @GetMapping("/{id}")
    public PlanModels.PlanResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.get(jwt.getSubject(), id, jwt.getTokenValue());
    }
    @PutMapping("/{id}")
    public PlanModels.PlanResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                          @Valid @RequestBody PlanModels.UpdatePlanRequest request) {
        return service.update(jwt.getSubject(), id, request, jwt.getTokenValue());
    }
    @PostMapping("/{id}/activate")
    public PlanModels.PlanResponse activate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                            @Valid @RequestBody PlanModels.TransitionRequest request) {
        return service.activate(jwt.getSubject(), id, request.version(), jwt.getTokenValue());
    }
    @PostMapping("/{id}/archive")
    public PlanModels.PlanResponse archive(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                           @Valid @RequestBody PlanModels.TransitionRequest request) {
        return service.archive(jwt.getSubject(), id, request.version());
    }
    @PostMapping("/{id}/apply-suggestion")
    public PlanModels.PlanResponse applySuggestion(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                   @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                   @Valid @RequestBody PlanModels.ApplySuggestionRequest request) {
        return service.applySuggestion(jwt.getSubject(), id, request, idempotencyKey, jwt.getTokenValue());
    }
}
