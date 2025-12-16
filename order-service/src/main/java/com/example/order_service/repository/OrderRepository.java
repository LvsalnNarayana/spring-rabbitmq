package com.example.order_service.repository;

import com.example.order_service.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    List<OrderEntity> findByStatus(OrderEntity.Status status);

    List<OrderEntity> findByPaymentStatus(OrderEntity.PaymentStatus paymentStatus);

    Optional<OrderEntity> findByIdAndStatus(UUID id, OrderEntity.Status status);

    Optional<OrderEntity> findByIdAndPaymentStatus(UUID id, OrderEntity.PaymentStatus paymentStatus);

    Optional<OrderEntity> findByIdAndStatusAndPaymentStatus(
            UUID id,
            OrderEntity.Status status,
            OrderEntity.PaymentStatus paymentStatus
    );
}
