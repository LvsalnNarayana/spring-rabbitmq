package com.example.message_processor.services;

import com.example.message_processor.utils.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderQueueListener {

    private static final String NOTIFICATION_SERVICE_BASE_URL =
            "http://localhost:6000";

    private static final String CHANNEL_IN_APP = "IN_APP";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /* =========================================================
       ORDER STATUS EVENTS
       ========================================================= */

    @RabbitListener(queues = "order.status.queue")
    public void handleOrderStatusEvents(String json) {
        try {
            JsonNode event = objectMapper.readTree(json);

            String status = event.path("status").asText(null);
            String orderId = event.path("orderId").asText(null);

            // Temporary fallback (until userId is part of event)
            String userId = event.path("userId").asText("user-123");

            if (status == null || orderId == null) {
                log.warn("Invalid ORDER STATUS event payload: {}", json);
                return;
            }

            switch (status) {

                case "CONFIRMED" -> sendNotification(
                        userId,
                        "ORDER_CREATED",
                        "Your order has been confirmed successfully.",
                        orderId
                );

                case "SHIPPED" -> sendNotification(
                        userId,
                        "ORDER_SHIPPED",
                        "Your order has been shipped and is on the way.",
                        orderId
                );

                case "CANCELLED" -> sendNotification(
                        userId,
                        "ORDER_CANCELLED",
                        "Your order has been cancelled.",
                        orderId
                );
            }

        } catch (Exception ex) {
            log.error("❌ Failed to process ORDER STATUS event", ex);
        }
    }

    /* =========================================================
       ORDER ANALYTICS EVENTS (NO USER NOTIFICATIONS)
       ========================================================= */

    @RabbitListener(queues = "order.analytics.queue")
    public void handleOrderAnalyticsEvents(String json) {
        // Analytics are system-facing only
        log.debug("📊 Order analytics event received (ignored for notifications)");
    }

    /* =========================================================
       PAYMENT STATUS EVENTS
       ========================================================= */

    @RabbitListener(queues = "payment.status.queue")
    public void handlePaymentStatusEvents(String json) {
        try {
            JsonNode event = objectMapper.readTree(json);

            String paymentStatus = event.path("paymentStatus").asText(null);
            String orderId = event.path("orderId").asText(null);
            String userId = event.path("userId").asText("user-123");

            if (paymentStatus == null || orderId == null) {
                log.warn("Invalid PAYMENT STATUS event payload: {}", json);
                return;
            }
            switch (paymentStatus) {

                case "FAILED" -> sendNotification(
                        userId,
                        "PAYMENT_FAILED",
                        "Payment failed for your order. Please retry.",
                        orderId
                );

                case "COMPLETED" -> sendNotification(
                        userId,
                        "PAYMENT_SUCCESS",
                        "Payment completed successfully for your order.",
                        orderId
                );
            }

        } catch (Exception ex) {
            log.error("❌ Failed to process PAYMENT STATUS event", ex);
        }
    }

    /* =========================================================
       NOTIFICATION DISPATCHER
       ========================================================= */

    private void sendNotification(
            String userId,
            String type,
            String content,
            String referenceId
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("type", type); // MUST MATCH ENUM EXACTLY
        payload.put("content", content);
        payload.put("channel", "IN_APP");
        payload.put("referenceId", UUID.fromString(referenceId));

        restClient.post(
                NOTIFICATION_SERVICE_BASE_URL,
                "/api/v1/notifications",
                null,
                payload,
                Void.class
        );

        log.info(
                "🔔 Notification sent → userId={}, type={}, referenceId={}",
                userId,
                type,
                referenceId
        );
    }

}
