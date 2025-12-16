package com.example.product_service.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import static com.example.product_service.configuration.RabbitMQConfig.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQSender {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /* =========================================================
       1. INVENTORY → WAREHOUSE (Topic Exchange)
       ========================================================= */

    public void sendInventoryUpdateToWarehouse(
            String warehouseId,
            Object payload
    ) {
        String routingKey = "products.warehouse." + warehouseId + ".queue";
        log.info(routingKey);
        sendAsJson(PRODUCTS_WAREHOUSE_TOPIC_EXCHANGE, routingKey, payload);
    }

    /* =========================================================
       2. INVENTORY → ANALYTICS (Fanout Exchange)
       ========================================================= */

    public void sendEventToAnalytics(Object payload) {
        sendAsJson(PRODUCTS_ANALYTICS_FANOUT_EXCHANGE, "", payload);
    }

    /* =========================================================
       3. PRODUCT UNAVAILABLE → DELAYED USER NOTIFICATION (TTL)
       ========================================================= */

    public void sendDelayedUserNotification(Object payload) {
        sendAsJson(PRODUCTS_NOTIFICATION_TTL_EXCHANGE, "notify.delay", payload);
    }

    /* =========================================================
       INTERNAL JSON SENDER
       ========================================================= */

    private void sendAsJson(String exchange, String routingKey, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            rabbitTemplate.convertAndSend(exchange, routingKey, json);

            log.info(
                    "Message sent as JSON → exchange={}, routingKey={}",
                    exchange, routingKey
            );
        } catch (Exception ex) {
            log.error("Failed to serialize payload to JSON", ex);
            throw new IllegalStateException("Message serialization failed", ex);
        }
    }
}
