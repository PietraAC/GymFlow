package com.gymflow.gym.unit;

import com.gymflow.gym.gym.GymAuthorizationService;
import com.gymflow.gym.gym.GymRepository;
import com.gymflow.gym.shared.api.PageRequestFactory;
import com.gymflow.gym.shared.api.PageResponse;
import com.gymflow.gym.shared.error.ResourceNotFoundException;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnitApplicationService {
    private static final Set<String> SORTS = Set.of("name", "city", "createdAt");
    private final GymUnitRepository units;
    private final GymRepository gyms;
    private final GymAuthorizationService authorization;

    public UnitApplicationService(GymUnitRepository units, GymRepository gyms, GymAuthorizationService authorization) {
        this.units = units;
        this.gyms = gyms;
        this.authorization = authorization;
    }

    @Transactional
    public UnitModels.UnitResponse create(UUID gymId, UnitModels.CreateUnitRequest request, String subject) {
        authorization.requireAdmin(gymId, subject);
        if (!gyms.existsById(gymId)) throw new ResourceNotFoundException("Academia não encontrada");
        GymUnit unit = units.save(new GymUnit(gymId, request.name().trim(), request.city().trim(), request.active() == null || request.active()));
        return UnitModels.UnitResponse.from(unit);
    }

    @Transactional(readOnly = true)
    public PageResponse<UnitModels.UnitResponse> list(UUID gymId, String subject, int page, int size, String sort) {
        if (!gyms.existsById(gymId)) throw new ResourceNotFoundException("Academia não encontrada");
        var pageable = PageRequestFactory.create(page, size, sort, SORTS);
        var result = authorization.isAdmin(gymId, subject) ? units.findByGymId(gymId, pageable) : units.findByGymIdAndActiveTrue(gymId, pageable);
        return PageResponse.from(result, UnitModels.UnitResponse::from);
    }

    @Transactional(readOnly = true)
    public UnitModels.UnitResponse get(UUID unitId, String subject) {
        GymUnit unit = requireVisible(unitId, subject);
        return UnitModels.UnitResponse.from(unit);
    }

    @Transactional
    public UnitModels.UnitResponse update(UUID unitId, UnitModels.UpdateUnitRequest request, String subject) {
        GymUnit unit = units.findById(unitId).orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada"));
        authorization.requireAdmin(unit.getGymId(), subject);
        unit.update(trim(request.name()), trim(request.city()), request.active());
        return UnitModels.UnitResponse.from(unit);
    }

    public GymUnit requireVisible(UUID unitId, String subject) {
        GymUnit unit = units.findById(unitId).orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada"));
        if (!unit.isActive() && !authorization.isAdmin(unit.getGymId(), subject)) {
            throw new ResourceNotFoundException("Unidade não encontrada");
        }
        return unit;
    }

    private String trim(String value) { return value == null ? null : value.trim(); }
}

