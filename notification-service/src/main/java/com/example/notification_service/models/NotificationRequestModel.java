package com.example.notification_service.models;

import com.example.notification_service.entity.NotificationEntity;
import lombok.*;

import java.util.UUID;

/* =========================
   NOTIFICATION REQUEST MODEL
   ========================= */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequestModel {

    private String userId;

    private NotificationEntity.NotificationType type;

    private String content;

    // Optional delivery channel override (EMAIL, SMS, PUSH, IN_APP)
    private String channel;

    // Reference to triggering entity (orderId, paymentId, productId, etc.)
    private UUID referenceId;

    /*
     * TODO:
     * - Add templateId + template variables
     * - Add priority field
     * - Add scheduledAt for delayed notifications
     */
}
