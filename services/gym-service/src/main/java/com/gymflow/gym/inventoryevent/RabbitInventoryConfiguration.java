package com.gymflow.gym.inventoryevent;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitInventoryConfiguration {
    @Bean
    TopicExchange inventoryExchange(@Value("${app.messaging.inventory.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }
}
