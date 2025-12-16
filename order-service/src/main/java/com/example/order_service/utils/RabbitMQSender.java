package com.example.order_service.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import static com.example.order_service.configuration.RabbitMQConfig.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQSender {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /* =========================================================
       1️⃣ ORDER STATUS EVENTS (Topic Exchange)
       ========================================================= */

    /**
     * Emits order lifecycle events.
     *
     * Routing key examples:
     * - order.status.created
     * - order.status.confirmed
     * - order.status.cancelled
     */
    public void sendOrderStatusEvent(
            String status,
            Object payload
    ) {
        String routingKey = "order.status." + status.toLowerCase();
        sendAsJson(ORDER_STATUS_TOPIC_EXCHANGE, routingKey, payload);
    }

    /* =========================================================
       2️⃣ ORDER ANALYTICS EVENTS (Fanout)
       ========================================================= */

    /**
     * Broadcasts order events to analytics systems.
     */
    public void sendOrderAnalyticsEvent(Object payload) {
        sendAsJson(ORDER_ANALYTICS_FANOUT_EXCHANGE, "", payload);
    }

    /* =========================================================
       3️⃣ PAYMENT STATUS EVENTS (Topic Exchange)
       ========================================================= */

    /**
     * Emits payment state transitions.
     *
     * Routing key examples:
     * - payment.status.initiated
     * - payment.status.failed
     * - payment.status.completed
     */
    public void sendPaymentStatusEvent(
            String paymentStatus,
            Object payload
    ) {
        String routingKey = "payment.status." + paymentStatus.toLowerCase();
        sendAsJson(PAYMENT_STATUS_TOPIC_EXCHANGE, routingKey, payload);
    }

    /* =========================================================
       INTERNAL JSON SENDER (STRICT)
       ========================================================= */

    private void sendAsJson(
            String exchange,
            String routingKey,
            Object payload
    ) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            rabbitTemplate.convertAndSend(exchange, routingKey, json);

            log.info(
                    "📨 Message sent → exchange={}, routingKey={}",
                    exchange,
                    routingKey
            );
        } catch (Exception ex) {
            log.error("❌ Failed to serialize/send message", ex);
            throw new IllegalStateException("RabbitMQ message publishing failed", ex);
        }
    }
}
