package com.example.product_service.models;

import com.example.product_service.entity.ProductEntity;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/* =========================
   PRODUCT RESPONSE MODEL
   ========================= */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ProductResponseModel {

    private UUID id;
    private String title;
    private String description;
    private BigDecimal price;
    private int availableQuantity;

    private String sku;
    private String category;
    private String brand;

    private ProductEntity.ProductStatus status;

    // Auditing
    private Instant createdAt;
    private Instant updatedAt;

    /*
     * TODO:
     * - Add image URLs
     * - Add price history snapshot
     * - Add availability flag
     */
}
