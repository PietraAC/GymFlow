package com.gymflow.workout.inventoryevent;

import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitInventoryConfiguration {
    @Bean TopicExchange inventoryExchange(@Value("${app.messaging.inventory.exchange}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean DirectExchange inventoryDeadLetterExchange(
        @Value("${app.messaging.inventory.dead-letter-exchange}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean Queue inventoryQueue(@Value("${app.messaging.inventory.queue}") String name,
                               @Value("${app.messaging.inventory.dead-letter-exchange}") String deadLetterExchange,
                               @Value("${app.messaging.inventory.dead-letter-routing-key}") String deadLetterRoutingKey) {
        return QueueBuilder.durable(name).withArguments(Map.of(
            "x-dead-letter-exchange", deadLetterExchange,
            "x-dead-letter-routing-key", deadLetterRoutingKey)).build();
    }

    @Bean Queue inventoryDeadLetterQueue(@Value("${app.messaging.inventory.dead-letter-queue}") String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean Binding inventoryBinding(@Qualifier("inventoryQueue") Queue inventoryQueue,
                                   TopicExchange inventoryExchange,
                                   @Value("${app.messaging.inventory.routing-key}") String routingKey) {
        return BindingBuilder.bind(inventoryQueue).to(inventoryExchange).with(routingKey);
    }

    @Bean Binding inventoryDeadLetterBinding(@Qualifier("inventoryDeadLetterQueue") Queue inventoryDeadLetterQueue,
                                             DirectExchange inventoryDeadLetterExchange,
                                             @Value("${app.messaging.inventory.dead-letter-routing-key}") String routingKey) {
        return BindingBuilder.bind(inventoryDeadLetterQueue).to(inventoryDeadLetterExchange).with(routingKey);
    }
}
