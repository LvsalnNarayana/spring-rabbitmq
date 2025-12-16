package com.example.product_service.models;

import com.example.product_service.entity.ProductEntity;
import lombok.*;

import java.math.BigDecimal;

/* =========================
   CREATE PRODUCT REQUEST
   ========================= */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProductRequestModel {

    private String title;
    private String description;
    private BigDecimal price;
    private int availableQuantity;

    private String sku;
    private String category;
    private String brand;

    /*
     * TODO:
     * - Add validation annotations (@NotBlank, @Positive)
     * - Add tax / discount metadata
     * - Add image URLs
     * - Prevent duplicate SKU at service layer
     */
}
