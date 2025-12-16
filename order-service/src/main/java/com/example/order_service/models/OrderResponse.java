package com.example.order_service.models;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private UUID orderId;
    private List<OrderProductResponse> products;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private AddressResponse address;

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderProductResponse {

        private UUID productId;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressResponse {

        private String line1;
        private String line2;
        private String city;
        private String state;
        private String country;
        private String postalCode;
    }

    public enum OrderStatus {
        CONFIRMED,
        SHIPPED,
        RECEIVED,
        CANCELLED
    }

    public enum PaymentStatus {
        PENDING,
        INITIATED,
        AUTHORIZED,
        PAID,
        FAILED,
        REFUNDED
    }
}
