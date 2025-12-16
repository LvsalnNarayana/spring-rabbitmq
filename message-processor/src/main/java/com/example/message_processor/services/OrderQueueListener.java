package com.example.message_processor.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderQueueListener {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /* =========================================================
       ORDER STATUS EVENTS
       ========================================================= */

    /**
     * Consumes order lifecycle events.
     *
     * Routing keys examples:
     * - order.status.created
     * - order.status.confirmed
     * - order.status.cancelled
     */
    @RabbitListener(queues = "order.status.queue")
    public void handleOrderStatusEvents(String json) {
        logEvent("📦 [ORDER-STATUS]", json);
    }

    /* =========================================================
       ORDER ANALYTICS EVENTS (Fanout)
       ========================================================= */

    /**
     * Consumes broadcasted analytics events.
     */
    @RabbitListener(queues = "order.analytics.queue")
    public void handleOrderAnalyticsEvents(String json) {
        logEvent("📊 [ORDER-ANALYTICS]", json);
    }

    /* =========================================================
       PAYMENT STATUS EVENTS
       ========================================================= */

    /**
     * Consumes payment lifecycle events.
     *
     * Routing keys examples:
     * - payment.status.initiated
     * - payment.status.failed
     * - payment.status.completed
     */
    @RabbitListener(queues = "payment.status.queue")
    public void handlePaymentStatusEvents(String json) {
        logEvent("💳 [PAYMENT-STATUS]", json);
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
