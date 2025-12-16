package com.example.order_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class OrderEntity {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "order_products",
            joinColumns = @JoinColumn(name = "order_id")
    )
    @Builder.Default
    private List<OrderProduct> products = new ArrayList<>();

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.CONFIRMED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // Unidirectional OneToOne → Address in separate table
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id")
    private Address address;

    // ===== Auditing =====

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ===== Enums =====

    public enum Status {
        CANCELLED,
        CONFIRMED,
        SHIPPED,
        RECEIVED
    }

    public enum PaymentStatus {
        PENDING,     // Order created, payment not initiated
        INITIATED,   // Payment flow started
        AUTHORIZED,  // Payment authorized but not captured
        PAID,        // Payment successfully completed
        FAILED,      // Payment attempt failed
        REFUNDED     // Payment refunded after cancellation/return
    }

    // ===== Value Objects =====

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderProduct {

        @Column(name = "product_id", nullable = false)
        private UUID productId;

        @Column(nullable = false)
        private int quantity;

        @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
        private BigDecimal unitPrice;
    }

    /*
     * TODO:
     * - Add payment reference / transactionId
     * - Add payment provider metadata (UPI, Card, Wallet, etc.)
     * - Enforce valid order ↔ payment state transitions
     * - Emit order & payment lifecycle events
     */
}
