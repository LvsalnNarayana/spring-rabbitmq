package com.example.order_service.services;

import com.example.order_service.entity.Address;
import com.example.order_service.entity.OrderEntity;
import com.example.order_service.models.InventoryBulkReduceRequest;
import com.example.order_service.models.InventoryReduceResponse;
import com.example.order_service.models.OrderCreateRequest;
import com.example.order_service.models.ProductSnapshot;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.utils.RabbitMQSender;
import com.example.order_service.utils.RestClient;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class OrderService {

    private static final String PRODUCT_SERVICE_BASE_URL = "http://localhost:5000";

    private final OrderRepository orderRepository;
    private final RestClient restClient;
    private final RabbitMQSender rabbitMQSender;

    public OrderService(OrderRepository orderRepository, RestClient restClient, RabbitMQSender rabbitMQSender) {
        this.orderRepository = orderRepository;
        this.restClient = restClient;
        this.rabbitMQSender = rabbitMQSender;
    }

    /* =========================
       CREATE ORDER
       ========================= */

    @Transactional
    public OrderEntity createOrder(OrderCreateRequest orderRequest) {

        OrderEntity order = OrderEntity.builder()
                .address(mapAddress(orderRequest))
                .products(mapProducts(orderRequest))
                .build();

        validateOrderStructure(order);

    /* =========================
       1️⃣ FETCH PRODUCT DATA
       ========================= */

        List<UUID> productIds = order.getProducts()
                .stream()
                .map(OrderEntity.OrderProduct::getProductId)
                .toList();

        ProductSnapshot[] products =
                restClient.post(
                        PRODUCT_SERVICE_BASE_URL,
                        "/api/v1/products/batch",
                        null,
                        productIds,
                        ProductSnapshot[].class
                );

        if (products == null || products.length != productIds.size()) {
            throw new IllegalStateException("Some products are missing or unavailable");
        }

        Map<UUID, ProductSnapshot> productMap =
                Arrays.stream(products)
                        .collect(Collectors.toMap(ProductSnapshot::getProductId, p -> p));

    /* =========================
       2️⃣ PRICE CALCULATION
       ========================= */

        BigDecimal total = BigDecimal.ZERO;

        for (OrderEntity.OrderProduct orderProduct : order.getProducts()) {

            ProductSnapshot product =
                    productMap.get(orderProduct.getProductId());

            if (product == null) {
                throw new IllegalStateException(
                        "Product not found: " + orderProduct.getProductId()
                );
            }

            if (!"ACTIVE".equals(product.getStatus())) {
                throw new IllegalStateException(
                        "Product not available: " + orderProduct.getProductId()
                );
            }

            BigDecimal lineTotal =
                    product.getPrice()
                            .multiply(BigDecimal.valueOf(orderProduct.getQuantity()));

            orderProduct.setUnitPrice(product.getPrice());
            total = total.add(lineTotal);
        }

    /* =========================
       3️⃣ FINALIZE & SAVE ORDER
       ========================= */

        order.setTotal(total);
        order.setStatus(OrderEntity.Status.CONFIRMED);
        order.setPaymentStatus(OrderEntity.PaymentStatus.PENDING);

        OrderEntity savedOrder = orderRepository.save(order);

    /* =========================
       4️⃣ REDUCE INVENTORY (BULK)
       ========================= */

        InventoryBulkReduceRequest inventoryRequest =
                new InventoryBulkReduceRequest(
                        order.getProducts().stream()
                                .map(p -> new InventoryBulkReduceRequest.Item(
                                        p.getProductId(),
                                        p.getQuantity()
                                ))
                                .toList()
                );

        InventoryReduceResponse response =
                restClient.post(
                        PRODUCT_SERVICE_BASE_URL,
                        "/api/v1/products/reduce",
                        null,
                        inventoryRequest,
                        InventoryReduceResponse.class
                );

        if (response == null || !"SUCCESS".equals(response.getStatus())) {
            throw new IllegalStateException("Inventory reduction failed for order " + savedOrder.getId());
        }

        log.info(
                "Order created successfully. orderId={}, total={}",
                savedOrder.getId(),
                total
        );
        rabbitMQSender.sendOrderStatusEvent(
                "confirmed",
                Map.of(
                        "orderId", savedOrder.getId(),
                        "status", savedOrder.getStatus(),
                        "total", savedOrder.getTotal(),
                        "timestamp", System.currentTimeMillis()
                )
        );
        rabbitMQSender.sendOrderAnalyticsEvent(
                Map.of(
                        "eventType", "ORDER_CREATED",
                        "orderId", savedOrder.getId(),
                        "total", savedOrder.getTotal()
                )
        );

        return savedOrder;
    }

    /* =========================
       PAYMENT STATUS UPDATE
       ========================= */

    @Transactional
    public OrderEntity updatePaymentStatus(
            UUID orderId,
            OrderEntity.PaymentStatus newPaymentStatus
    ) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Order not found: " + orderId)
                );

        if (!isValidPaymentTransition(order.getPaymentStatus(), newPaymentStatus)) {
            throw new IllegalStateException(
                    "Invalid payment status transition from "
                            + order.getPaymentStatus() + " to " + newPaymentStatus
            );
        }

        order.setPaymentStatus(newPaymentStatus);
        rabbitMQSender.sendPaymentStatusEvent(
                newPaymentStatus.name(),
                Map.of(
                        "orderId", orderId,
                        "paymentStatus", newPaymentStatus,
                        "timestamp", System.currentTimeMillis()
                )
        );
        rabbitMQSender.sendOrderAnalyticsEvent(
                Map.of(
                        "eventType", "PAYMENT_STATUS_UPDATED",
                        "orderId", orderId,
                        "paymentStatus", newPaymentStatus
                )
        );

        return orderRepository.save(order);
    }

    private boolean isValidPaymentTransition(
            OrderEntity.PaymentStatus current,
            OrderEntity.PaymentStatus next
    ) {
        return switch (current) {
            case PENDING -> next == OrderEntity.PaymentStatus.INITIATED
                    || next == OrderEntity.PaymentStatus.FAILED;
            case INITIATED -> next == OrderEntity.PaymentStatus.AUTHORIZED
                    || next == OrderEntity.PaymentStatus.FAILED;
            case AUTHORIZED -> next == OrderEntity.PaymentStatus.PAID
                    || next == OrderEntity.PaymentStatus.FAILED;
            case PAID -> next == OrderEntity.PaymentStatus.REFUNDED;
            case FAILED, REFUNDED -> false;
        };
    }

    /* =========================
       ORDER STATUS UPDATE
       ========================= */

    @Transactional
    public OrderEntity updateOrderStatus(
            UUID orderId,
            OrderEntity.Status newStatus
    ) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Order not found: " + orderId)
                );

        if (!isValidOrderTransition(order.getStatus(), newStatus)) {
            throw new IllegalStateException(
                    "Invalid order status transition from "
                            + order.getStatus() + " to " + newStatus
            );
        }

        order.setStatus(newStatus);
        rabbitMQSender.sendOrderStatusEvent(
                newStatus.name(),
                Map.of(
                        "orderId", orderId,
                        "status", newStatus,
                        "timestamp", System.currentTimeMillis()
                )
        );
        rabbitMQSender.sendOrderAnalyticsEvent(
                Map.of(
                        "eventType", "ORDER_STATUS_UPDATED",
                        "orderId", orderId,
                        "status", newStatus
                )
        );

        return orderRepository.save(order);
    }

    private boolean isValidOrderTransition(
            OrderEntity.Status current,
            OrderEntity.Status next
    ) {
        return switch (current) {
            case CONFIRMED -> next == OrderEntity.Status.SHIPPED
                    || next == OrderEntity.Status.CANCELLED;
            case SHIPPED -> next == OrderEntity.Status.RECEIVED;
            case CANCELLED, RECEIVED -> false;
        };
    }

    /* =========================
       READ OPERATIONS
       ========================= */

    public OrderEntity getOrderById(UUID id) {
        rabbitMQSender.sendOrderAnalyticsEvent(
                Map.of(
                        "eventType", "ORDER_FETCHED",
                        "orderId", id
                )
        );
        return orderRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Order not found: " + id)
                );
    }

    public List<OrderEntity> getOrdersByStatus(OrderEntity.Status status) {
        return orderRepository.findByStatus(status);
    }

    public List<OrderEntity> getOrdersByPaymentStatus(
            OrderEntity.PaymentStatus paymentStatus
    ) {
        return orderRepository.findByPaymentStatus(paymentStatus);
    }

    /* =========================
       VALIDATION
       ========================= */

    private void validateOrderStructure(OrderEntity order) {

        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        if (order.getAddress() == null) {
            throw new IllegalArgumentException("Delivery address is required");
        }

        if (order.getProducts() == null || order.getProducts().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one product");
        }

        long uniqueCount = order.getProducts().stream()
                .map(OrderEntity.OrderProduct::getProductId)
                .distinct()
                .count();

        if (uniqueCount < order.getProducts().size()) {
            throw new IllegalArgumentException(
                    "Duplicate products in the same order are not allowed"
            );
        }

        order.getProducts().forEach(p -> {
            if (p.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be greater than zero"
                );
            }
        });
    }

    /* =========================
       MAPPERS
       ========================= */

    private Address mapAddress(OrderCreateRequest request) {
        OrderCreateRequest.AddressRequest a = request.getAddress();
        return Address.builder()
                .line1(a.getLine1())
                .line2(a.getLine2())
                .city(a.getCity())
                .state(a.getState())
                .country(a.getCountry())
                .postalCode(a.getPostalCode())
                .build();
    }

    private List<OrderEntity.OrderProduct> mapProducts(
            OrderCreateRequest request
    ) {
        return request.getProducts().stream()
                .map(p -> OrderEntity.OrderProduct.builder()
                        .productId(p.getProductId())
                        .quantity(p.getQuantity())
                        .build())
                .collect(Collectors.toList());
    }
}
