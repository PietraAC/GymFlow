package com.gymflow.gym.unit;

import com.gymflow.gym.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class UnitController {
    private final UnitApplicationService service;
    public UnitController(UnitApplicationService service) { this.service = service; }

    @PostMapping("/gyms/{gymId}/units")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GYM_ADMIN')")
    @Operation(summary = "Cria uma unidade na academia administrada")
    public UnitModels.UnitResponse create(@PathVariable UUID gymId, @Valid @RequestBody UnitModels.CreateUnitRequest request,
                                          @AuthenticationPrincipal Jwt jwt) {
        return service.create(gymId, request, jwt.getSubject());
    }

    @GetMapping("/gyms/{gymId}/units")
    public PageResponse<UnitModels.UnitResponse> list(@PathVariable UUID gymId, @AuthenticationPrincipal Jwt jwt,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      @RequestParam(defaultValue = "name,asc") String sort) {
        return service.list(gymId, jwt.getSubject(), page, size, sort);
    }

    @GetMapping("/units/{unitId}")
    public UnitModels.UnitResponse get(@PathVariable UUID unitId, @AuthenticationPrincipal Jwt jwt) {
        return service.get(unitId, jwt.getSubject());
    }

    @PatchMapping("/units/{unitId}")
    @PreAuthorize("hasRole('GYM_ADMIN')")
    public UnitModels.UnitResponse update(@PathVariable UUID unitId, @Valid @RequestBody UnitModels.UpdateUnitRequest request,
                                          @AuthenticationPrincipal Jwt jwt) {
        return service.update(unitId, request, jwt.getSubject());
    }
}
