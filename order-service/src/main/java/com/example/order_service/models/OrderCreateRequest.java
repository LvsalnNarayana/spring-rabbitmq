package com.example.order_service.models;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateRequest {

    private AddressRequest address;
    private List<OrderProductRequest> products;

    private PaymentMethod paymentMethod;

    // Optional for now (will be owned by Payment Service later)
    private PaymentStatus paymentStatus;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderProductRequest {

        private UUID productId;
        private int quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressRequest {

        private String line1;
        private String line2;
        private String city;
        private String state;
        private String country;
        private String postalCode;
    }

    public enum PaymentStatus {
        PENDING,
        INITIATED,
        AUTHORIZED,
        PAID,
        FAILED
    }

    public enum PaymentMethod {
        CARD,
        UPI,
        NET_BANKING,
        WALLET,
        CASH_ON_DELIVERY
    }
}
