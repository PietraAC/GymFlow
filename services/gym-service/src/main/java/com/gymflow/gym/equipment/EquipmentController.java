package com.gymflow.gym.equipment;

import com.gymflow.gym.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class EquipmentController {
    private final EquipmentApplicationService service;
    public EquipmentController(EquipmentApplicationService service) { this.service = service; }

    @GetMapping("/equipment-types")
    public PageResponse<EquipmentModels.EquipmentTypeResponse> listTypes(@RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "50") int size,
                                                                         @RequestParam(defaultValue = "name,asc") String sort) {
        return service.listTypes(page, size, sort);
    }

    @GetMapping("/units/{unitId}/equipment")
    public List<EquipmentModels.UnitEquipmentResponse> listInventory(@PathVariable UUID unitId,
                                                                      @AuthenticationPrincipal Jwt jwt) {
        return service.listInventory(unitId, jwt.getSubject());
    }

    @PutMapping("/units/{unitId}/equipment/{equipmentTypeId}")
    @PreAuthorize("hasRole('GYM_ADMIN')")
    @Operation(summary = "Cria ou atualiza um item do inventário com controle otimista")
    public EquipmentModels.UnitEquipmentResponse upsert(@PathVariable UUID unitId, @PathVariable UUID equipmentTypeId,
                                                         @Valid @RequestBody EquipmentModels.UpsertUnitEquipmentRequest request,
                                                         @AuthenticationPrincipal Jwt jwt) {
        return service.upsert(unitId, equipmentTypeId, request, jwt.getSubject());
    }
}
