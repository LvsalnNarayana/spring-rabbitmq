package com.example.notification_service.controller;

import com.example.notification_service.entity.NotificationEntity;
import com.example.notification_service.models.NotificationRequestModel;
import com.example.notification_service.models.NotificationResponseModel;
import com.example.notification_service.services.NotificationServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(NotificationController.API_V1_NOTIFICATIONS)
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    public static final String API_V1 = "/v1";
    public static final String NOTIFICATIONS = "/notifications";
    public static final String API_V1_NOTIFICATIONS = API_V1 + NOTIFICATIONS;

    private final NotificationServices notificationServices;

    /* =========================
       CREATE
       ========================= */

    @PostMapping
    public ResponseEntity<NotificationResponseModel> createNotification(
            @RequestBody NotificationRequestModel request
    ) {
        log.info("Create notification request received");
        NotificationResponseModel response =
                notificationServices.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /* =========================
       READ
       ========================= */

    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponseModel> getNotificationById(
            @PathVariable UUID notificationId
    ) {
        return ResponseEntity.ok(
                notificationServices.getNotificationById(notificationId)
        );
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponseModel>> getNotifications(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) NotificationEntity.NotificationStatus status
    ) {
        if (userId != null) {
            return ResponseEntity.ok(
                    notificationServices.getNotificationsByUser(userId)
            );
        }

        if (status != null) {
            return ResponseEntity.ok(
                    notificationServices.getNotificationsByStatus(status)
            );
        }

        // TODO:
        // - Add pagination
        // - Add combined filters (userId + status)
        return ResponseEntity.ok(
                notificationServices.getNotificationsByStatus(
                        NotificationEntity.NotificationStatus.PENDING
                )
        );
    }

    /* =========================
       STATE UPDATES
       ========================= */

    @PatchMapping("/{notificationId}/sent")
    public ResponseEntity<NotificationResponseModel> markAsSent(
            @PathVariable UUID notificationId
    ) {
        return ResponseEntity.ok(
                notificationServices.markAsSent(notificationId)
        );
    }

    @PatchMapping("/{notificationId}/failed")
    public ResponseEntity<NotificationResponseModel> markAsFailed(
            @PathVariable UUID notificationId
    ) {
        return ResponseEntity.ok(
                notificationServices.markAsFailed(notificationId)
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponseModel> markAsRead(
            @PathVariable UUID notificationId
    ) {
        return ResponseEntity.ok(
                notificationServices.markAsRead(notificationId)
        );
    }

    /*
     * TODO:
     * - Add pagination & sorting
     * - Add bulk state updates
     * - Add OpenAPI / Swagger annotations
     * - Secure endpoints (user vs system roles)
     * - Add webhook / event-based notification triggers
     */
}
