package com.gymflow.workout.inventoryevent;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventConsumer {
    private final InventoryEventProcessor processor;
    private final ObjectMapper mapper;

    public InventoryEventConsumer(InventoryEventProcessor processor, ObjectMapper mapper) {
        this.processor = processor;
        this.mapper = mapper;
    }

    @RabbitListener(queues = "${app.messaging.inventory.queue}")
    public void receive(byte[] payload) {
        try {
            processor.process(mapper.readValue(payload, EquipmentAvailabilityChanged.class));
        } catch (IOException | IllegalArgumentException exception) {
            throw new AmqpRejectAndDontRequeueException("Evento de inventario invalido", exception);
        }
    }

}
