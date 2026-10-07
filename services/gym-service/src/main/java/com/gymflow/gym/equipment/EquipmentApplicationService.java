package com.gymflow.gym.equipment;

import com.gymflow.gym.gym.GymAuthorizationService;
import com.gymflow.gym.shared.api.PageRequestFactory;
import com.gymflow.gym.shared.api.PageResponse;
import com.gymflow.gym.shared.error.ConflictException;
import com.gymflow.gym.shared.error.ResourceNotFoundException;
import com.gymflow.gym.unit.GymUnit;
import com.gymflow.gym.unit.UnitApplicationService;
import com.gymflow.gym.unit.GymUnitRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentApplicationService {
    private static final Set<String> TYPE_SORTS = Set.of("name", "createdAt");
    private final EquipmentTypeRepository types;
    private final UnitEquipmentRepository inventory;
    private final GymUnitRepository units;
    private final UnitApplicationService unitService;
    private final GymAuthorizationService authorization;

    public EquipmentApplicationService(EquipmentTypeRepository types, UnitEquipmentRepository inventory,
                                       GymUnitRepository units, UnitApplicationService unitService,
                                       GymAuthorizationService authorization) {
        this.types = types;
        this.inventory = inventory;
        this.units = units;
        this.unitService = unitService;
        this.authorization = authorization;
    }

    @Transactional(readOnly = true)
    public PageResponse<EquipmentModels.EquipmentTypeResponse> listTypes(int page, int size, String sort) {
        return PageResponse.from(types.findByActiveTrue(PageRequestFactory.create(page, size, sort, TYPE_SORTS)),
            EquipmentModels.EquipmentTypeResponse::from);
    }

    @Transactional(readOnly = true)
    public List<EquipmentModels.UnitEquipmentResponse> listInventory(UUID unitId, String subject) {
        unitService.requireVisible(unitId, subject);
        List<UnitEquipment> items = inventory.findByUnitIdOrderByEquipmentTypeId(unitId);
        Map<UUID, EquipmentType> typeMap = types.findAllById(items.stream().map(UnitEquipment::getEquipmentTypeId).toList())
            .stream().collect(Collectors.toMap(EquipmentType::getId, Function.identity()));
        return items.stream().filter(item -> typeMap.containsKey(item.getEquipmentTypeId()))
            .map(item -> EquipmentModels.UnitEquipmentResponse.from(item, typeMap.get(item.getEquipmentTypeId())))
            .sorted(Comparator.comparing(EquipmentModels.UnitEquipmentResponse::equipmentTypeName)).toList();
    }

    @Transactional
    public EquipmentModels.UnitEquipmentResponse upsert(UUID unitId, UUID equipmentTypeId,
                                                         EquipmentModels.UpsertUnitEquipmentRequest request, String subject) {
        GymUnit unit = units.findById(unitId).orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada"));
        authorization.requireAdmin(unit.getGymId(), subject);
        EquipmentType type = types.findById(equipmentTypeId)
            .filter(EquipmentType::isActive)
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de equipamento ativo não encontrado"));
        if (request.availableQuantity() > request.totalQuantity()) {
            throw new IllegalArgumentException("A quantidade disponível não pode exceder a quantidade total");
        }
        UnitEquipment item = inventory.findByUnitIdAndEquipmentTypeId(unitId, equipmentTypeId).orElse(null);
        if (item == null) {
            if (request.version() != null && request.version() != 0) {
                throw new ConflictException("O version deve ser omitido ao criar um item de inventário");
            }
            item = new UnitEquipment(unitId, equipmentTypeId, request.totalQuantity(), request.availableQuantity(), request.notes());
        } else {
            if (request.version() == null || request.version() != item.getVersion()) {
                throw new ConflictException("Inventário alterado por outra operação; recarregue os dados");
            }
            item.update(request.totalQuantity(), request.availableQuantity(), request.notes());
        }
        return EquipmentModels.UnitEquipmentResponse.from(inventory.saveAndFlush(item), type);
    }
}

