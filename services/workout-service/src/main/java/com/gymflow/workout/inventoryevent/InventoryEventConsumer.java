package com.gymflow.workout.inventoryevent;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventConsumer {
    private final InventoryEventProcessor processor;
    private final ObjectMapper mapper;
    private final MeterRegistry metrics;

    public InventoryEventConsumer(InventoryEventProcessor processor, ObjectMapper mapper, MeterRegistry metrics) {
        this.processor = processor;
        this.mapper = mapper;
        this.metrics = metrics;
    }

    @RabbitListener(queues = "${app.messaging.inventory.queue}")
    public void receive(Message message) {
        String correlationId = correlationId(message);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", correlationId)) {
            try {
                processor.process(mapper.readValue(message.getBody(), EquipmentAvailabilityChanged.class));
                metrics.counter("gymflow.inventory.events", "outcome", "processed").increment();
            } catch (IOException | IllegalArgumentException exception) {
                metrics.counter("gymflow.inventory.events", "outcome", "rejected").increment();
                throw new AmqpRejectAndDontRequeueException("Evento de inventario invalido", exception);
            } catch (RuntimeException exception) {
                metrics.counter("gymflow.inventory.events", "outcome", "failed").increment();
                throw exception;
            }
        }
    }

    private String correlationId(Message message) {
        Object header = message.getMessageProperties().getHeaders().get("X-Correlation-Id");
        String candidate = header == null ? message.getMessageProperties().getCorrelationId() : header.toString();
        return candidate != null && candidate.matches("[A-Za-z0-9._-]{1,100}") ? candidate : "inventory-event";
    }

}
