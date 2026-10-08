package com.gymflow.workout.inventoryevent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryEventProcessor {
    private static final Logger log = LoggerFactory.getLogger(InventoryEventProcessor.class);
    private final InventoryEventRepository repository;

    public InventoryEventProcessor(InventoryEventRepository repository) { this.repository = repository; }

    @Transactional
    public void process(EquipmentAvailabilityChanged event) {
        event.validate();
        if (!repository.recordProcessed(event)) return;
        if (!repository.acceptNewerVersion(event)) return;
        int plans = repository.flagPotentiallyAffectedPlans(event);
        log.info("Inventory event processed eventId={} aggregateVersion={} potentiallyAffectedPlans={}",
            event.eventId(), event.aggregateVersion(), plans);
    }
}
