package com.example.notification_service.services;

import com.example.notification_service.entity.NotificationEntity;
import com.example.notification_service.models.NotificationRequestModel;
import com.example.notification_service.models.NotificationResponseModel;
import com.example.notification_service.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationServices {

    private final NotificationRepository notificationRepository;

    /* =========================
       CREATE / SEND
       ========================= */

    @Transactional
    public NotificationResponseModel createNotification(
            NotificationRequestModel request
    ) {
        NotificationEntity notification = NotificationEntity.builder()
                .userId(request.getUserId())
                .type(NotificationEntity.NotificationType.valueOf(String.valueOf(request.getType())))
                .content(request.getContent())
                .channel(request.getChannel())
                .referenceId(
                        request.getReferenceId() != null
                                ? request.getReferenceId().toString()
                                : null
                )
                .status(NotificationEntity.NotificationStatus.PENDING)
                .build();

        NotificationEntity saved = notificationRepository.save(notification);

        log.info(
                "Notification created. notificationId={}, userId={}, type={}",
                saved.getId(),
                saved.getUserId(),
                saved.getType()
        );

        /*
         * TODO:
         * - Publish event to notification-delivery worker
         * - Integrate EMAIL / SMS / PUSH providers
         */

        return mapToResponse(saved);
    }

    /* =========================
       READ
       ========================= */

    public NotificationResponseModel getNotificationById(UUID notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Notification not found: " + notificationId
                        )
                );

        return mapToResponse(notification);
    }

    public List<NotificationResponseModel> getNotificationsByUser(String userId) {
        return notificationRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<NotificationResponseModel> getNotificationsByStatus(
            NotificationEntity.NotificationStatus status
    ) {
        return notificationRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /* =========================
       STATE UPDATES
       ========================= */

    @Transactional
    public NotificationResponseModel markAsSent(UUID notificationId) {
        NotificationEntity notification = getNotificationEntity(notificationId);
        notification.setStatus(NotificationEntity.NotificationStatus.SENT);
        return mapToResponse(notificationRepository.save(notification));
    }

    @Transactional
    public NotificationResponseModel markAsFailed(UUID notificationId) {
        NotificationEntity notification = getNotificationEntity(notificationId);
        notification.setStatus(NotificationEntity.NotificationStatus.FAILED);
        return mapToResponse(notificationRepository.save(notification));
    }

    @Transactional
    public NotificationResponseModel markAsRead(UUID notificationId) {
        NotificationEntity notification = getNotificationEntity(notificationId);
        notification.setStatus(NotificationEntity.NotificationStatus.READ);
        return mapToResponse(notificationRepository.save(notification));
    }

    /* =========================
       INTERNAL HELPERS
       ========================= */

    private NotificationEntity getNotificationEntity(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Notification not found: " + notificationId
                        )
                );
    }

    private NotificationResponseModel mapToResponse(
            NotificationEntity notification
    ) {
        return NotificationResponseModel.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .content(notification.getContent())
                .status(notification.getStatus())
                .channel(notification.getChannel())
                .referenceId(notification.getReferenceId())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }

    /*
     * TODO:
     * - Add retry mechanism for FAILED notifications
     * - Add scheduled / delayed notifications
     * - Add bulk status updates
     * - Add event-driven consumption (Kafka / RabbitMQ)
     */
}
