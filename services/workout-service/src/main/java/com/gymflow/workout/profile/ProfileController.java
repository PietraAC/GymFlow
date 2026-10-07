package com.gymflow.workout.profile;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/me/profile") @PreAuthorize("hasRole('STUDENT')") @SecurityRequirement(name = "bearerAuth")
public class ProfileController {
    private final ProfileService service;
    public ProfileController(ProfileService service) { this.service = service; }
    @GetMapping public ProfileModels.ProfileResponse get(@AuthenticationPrincipal Jwt jwt) { return service.get(jwt.getSubject()); }
    @PutMapping public ProfileModels.ProfileResponse upsert(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileModels.ProfileRequest request) { return service.upsert(jwt.getSubject(), request); }
}
