package com.example.notification_service.models;

import com.example.notification_service.entity.NotificationEntity;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/* =========================
   NOTIFICATION RESPONSE MODEL
   ========================= */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseModel {

    private UUID id;

    private String userId;

    private NotificationEntity.NotificationType type;

    private String content;

    private NotificationEntity.NotificationStatus status;

    private String channel;

    private String referenceId;

    // Auditing
    private Instant createdAt;
    private Instant updatedAt;

    /*
     * TODO:
     * - Add delivery provider response
     * - Add readAt timestamp
     * - Add retry metadata
     */
}
