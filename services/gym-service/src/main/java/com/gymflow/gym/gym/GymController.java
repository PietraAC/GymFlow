package com.gymflow.gym.gym;

import com.gymflow.gym.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gyms")
@SecurityRequirement(name = "bearerAuth")
public class GymController {
    private final GymApplicationService service;

    public GymController(GymApplicationService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GYM_ADMIN')")
    @Operation(summary = "Cria uma academia e vincula o administrador autenticado")
    public GymModels.GymResponse create(@Valid @RequestBody GymModels.CreateGymRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        return service.create(request, jwt.getSubject());
    }

    @GetMapping
    @Operation(summary = "Lista academias visíveis ao usuário")
    public PageResponse<GymModels.GymResponse> list(@AuthenticationPrincipal Jwt jwt, Authentication authentication,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @RequestParam(defaultValue = "name,asc") String sort) {
        boolean administrator = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_GYM_ADMIN".equals(a.getAuthority()));
        return service.list(jwt.getSubject(), administrator, page, size, sort);
    }
}

