package com.example.notification_service.repository;

import com.example.notification_service.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByUserId(String userId);

    List<NotificationEntity> findByStatus(NotificationEntity.NotificationStatus status);

    List<NotificationEntity> findByUserIdAndStatus(
            String userId,
            NotificationEntity.NotificationStatus status
    );

    List<NotificationEntity> findByReferenceId(String referenceId);

    /*
     * TODO:
     * - Add paging queries for inbox-style notifications
     * - Add retry-based queries (FAILED / PENDING)
     * - Add custom JPQL for bulk status updates
     */
}
