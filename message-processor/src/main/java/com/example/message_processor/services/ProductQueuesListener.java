package com.example.message_processor.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductQueuesListener {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /* =========================================================
       ANALYTICS EVENTS (Fanout)
       ========================================================= */

    @RabbitListener(queues = "products.analytics.queue")
    public void handleAnalyticsEvents(String json) {
        logEvent("📊 [ANALYTICS]", json);
    }

    /* =========================================================
       WAREHOUSE EVENTS
       ========================================================= */

    @RabbitListener(queues = "products.warehouse.BLRA.queue")
    public void handleWarehouseEvents(String json) {
        logEvent("🏭 [WAREHOUSE]", json);
    }

    /* =========================================================
       USER NOTIFICATION EVENTS (AFTER TTL)
       ========================================================= */

    @RabbitListener(queues = "products.notification.user.queue")
    public void handleUserNotificationEvents(String json) {
        logEvent("🔔 [USER-NOTIFICATION]", json);
    }

    /* =========================================================
       INTERNAL HELPER
       ========================================================= */

    private void logEvent(String prefix, String json) {
        try {
            Object parsed = objectMapper.readValue(json, Object.class);
            String pretty = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(parsed);

            log.info("{} Event received:\n{}", prefix, pretty);
        } catch (Exception ex) {
            log.warn("{} Raw message received (not JSON): {}", prefix, json);
        }
    }
}
