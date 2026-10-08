package com.gymflow.gym.inventoryevent;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxRepository repository;
    private final RabbitTemplate rabbit;
    private final String exchange;
    private final Duration confirmTimeout;
    private final int batchSize;

    public OutboxPublisher(OutboxRepository repository, RabbitTemplate rabbit,
                           @Value("${app.messaging.inventory.exchange}") String exchange,
                           @Value("${app.messaging.inventory.confirm-timeout}") Duration confirmTimeout,
                           @Value("${app.messaging.inventory.batch-size}") int batchSize) {
        this.repository = repository;
        this.rabbit = rabbit;
        this.exchange = exchange;
        this.confirmTimeout = confirmTimeout;
        this.batchSize = batchSize;
        this.rabbit.setMandatory(true);
    }

    @Scheduled(fixedDelayString = "${app.messaging.inventory.publish-delay}")
    public synchronized void publishPending() {
        repository.pending(batchSize).forEach(this::publish);
    }

    void publish(OutboxRepository.PendingEvent event) {
        try {
            CorrelationData correlation = new CorrelationData(event.eventId().toString());
            Message message = MessageBuilder.withBody(event.payload().getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(event.eventId().toString())
                .setHeader("eventType", EquipmentAvailabilityChanged.TYPE)
                .build();
            rabbit.send(exchange, event.routingKey(), message, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture()
                .get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw new IllegalStateException(confirm.getReason() == null ? "Mensagem nao roteada" : confirm.getReason());
            }
            repository.markPublished(event.id());
        } catch (Exception exception) {
            String category = exception.getClass().getSimpleName();
            repository.markFailed(event.id(), event.attempts(), category.substring(0, Math.min(category.length(), 500)));
            log.warn("Inventory outbox publish failed eventId={} category={}", event.eventId(), category);
        }
    }
}
