package com.example.order_service.controller;

import com.example.order_service.entity.OrderEntity;
import com.example.order_service.models.OrderCreateRequest;
import com.example.order_service.models.OrderResponse;
import com.example.order_service.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(OrderController.API_V1_ORDERS)
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    public static final String API_V1 = "/v1";
    public static final String ORDERS = "/orders";
    public static final String API_V1_ORDERS = API_V1 + ORDERS;

    private final OrderService orderService;

    /* =========================
       CREATE ORDER
       ========================= */

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody OrderCreateRequest request
    ) {
        log.info("Create order request received");

        OrderEntity savedOrder = orderService.createOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapToResponse(savedOrder));
    }

    /* =========================
       READ OPERATIONS
       ========================= */

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable UUID orderId
    ) {
        OrderEntity order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(mapToResponse(order));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(
            @RequestParam(required = false) OrderEntity.Status status
    ) {
        List<OrderEntity> orders =
                (status == null)
                        ? orderService.getOrdersByStatus(OrderEntity.Status.CONFIRMED)
                        : orderService.getOrdersByStatus(status);

        return ResponseEntity.ok(
                orders.stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList())
        );
    }

    /* =========================
       ORDER STATUS UPDATE
       ========================= */

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestParam OrderEntity.Status status
    ) {
        OrderEntity updatedOrder =
                orderService.updateOrderStatus(orderId, status);

        return ResponseEntity.ok(mapToResponse(updatedOrder));
    }

    /* =========================
       PAYMENT STATUS UPDATE
       ========================= */

    @PatchMapping("/{orderId}/payment-status")
    public ResponseEntity<OrderResponse> updatePaymentStatus(
            @PathVariable UUID orderId,
            @RequestParam OrderEntity.PaymentStatus paymentStatus
    ) {
        OrderEntity updatedOrder =
                orderService.updatePaymentStatus(orderId, paymentStatus);

        return ResponseEntity.ok(mapToResponse(updatedOrder));
    }

    /* =========================
       RESPONSE MAPPERS
       ========================= */

    private OrderResponse mapToResponse(OrderEntity order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .status(OrderResponse.OrderStatus.valueOf(
                        order.getStatus().name()
                ))
                .paymentStatus(OrderResponse.PaymentStatus.valueOf(
                        order.getPaymentStatus().name()
                ))
                .totalAmount(order.getTotal())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .address(mapAddressResponse(order))
                .products(
                        order.getProducts().stream()
                                .map(p -> OrderResponse.OrderProductResponse.builder()
                                        .productId(p.getProductId())
                                        .quantity(p.getQuantity())
                                        .unitPrice(p.getUnitPrice())
                                        .lineTotal(
                                                p.getUnitPrice()
                                                        .multiply(BigDecimal.valueOf(p.getQuantity()))
                                        )
                                        .build()
                                )
                                .collect(Collectors.toList())
                )
                .build();
    }

    private OrderResponse.AddressResponse mapAddressResponse(OrderEntity order) {
        if (order.getAddress() == null) {
            return null;
        }

        return OrderResponse.AddressResponse.builder()
                .line1(order.getAddress().getLine1())
                .line2(order.getAddress().getLine2())
                .city(order.getAddress().getCity())
                .state(order.getAddress().getState())
                .country(order.getAddress().getCountry())
                .postalCode(order.getAddress().getPostalCode())
                .build();
    }

    /*
     * TODO (Future Enhancements):
     * - Move mapping logic to MapStruct
     * - Add pagination & sorting
     * - Add authentication & authorization
     * - Add idempotency key (Order-Idempotency-Key header)
     * - Emit ORDER_CREATED / PAYMENT_UPDATED events
     * - Add OpenAPI / Swagger annotations
     */
}
