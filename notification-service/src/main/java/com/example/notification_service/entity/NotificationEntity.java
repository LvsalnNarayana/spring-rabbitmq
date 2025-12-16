package com.example.notification_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class NotificationEntity {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId; // recipient user identifier

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(length = 100)
    private String channel; // EMAIL, SMS, PUSH, IN_APP (optional string for flexibility)

    @Column(length = 100)
    private String referenceId; // orderId, paymentId, etc.

    // ===== Auditing =====

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ===== Enums =====

    public enum NotificationType {
        ORDER_CREATED,
        ORDER_SHIPPED,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        PRODUCT_OUT_OF_STOCK,
        GENERIC
    }

    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED,
        READ
    }

    /*
     * TODO:
     * - Add retryCount & nextRetryAt
     * - Add payload JSON column for templates
     * - Add delivery provider metadata
     * - Emit notification delivery events
     * - Add @Version for optimistic locking
     */
}
