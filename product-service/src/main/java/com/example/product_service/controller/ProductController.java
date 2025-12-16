package com.example.product_service.controller;

import com.example.product_service.entity.ProductEntity;
import com.example.product_service.models.BulkInventoryReductionRequest;
import com.example.product_service.models.BulkInventoryReductionResponse;
import com.example.product_service.models.CreateProductRequestModel;
import com.example.product_service.models.ProductResponseModel;
import com.example.product_service.services.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ProductController.API_V1_PRODUCTS)
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    public static final String API_V1 = "/v1";
    public static final String PRODUCTS = "/products";
    public static final String API_V1_PRODUCTS = API_V1 + PRODUCTS;

    private final ProductService productService;

    /* =========================
       CREATE
       ========================= */

    @PostMapping
    public ResponseEntity<ProductResponseModel> createProduct(
            @RequestBody CreateProductRequestModel request
    ) {
        log.info("Create product request received");
        ProductResponseModel response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /* =========================
       READ
       ========================= */

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseModel> getProductById(
            @PathVariable UUID productId
    ) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<ProductResponseModel>> getProductsByIds(
            @RequestBody List<UUID> productIds
    ) {
        return ResponseEntity.ok(
                productService.getProductsByIdsStrict(productIds)
        );
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseModel>> getProducts(
            @RequestParam(required = false) ProductEntity.ProductStatus status,
            @RequestParam(required = false) String category
    ) {
        if (status != null) {
            return ResponseEntity.ok(productService.getProductsByStatus(status));
        }

        if (category != null) {
            return ResponseEntity.ok(productService.getProductsByCategory(category));
        }

        // Default: active products
        return ResponseEntity.ok(
                productService.getProductsByStatus(ProductEntity.ProductStatus.ACTIVE)
        );
    }

    /* =========================
       INVENTORY OPERATIONS
       ========================= */
  /* =========================================================
       BULK INVENTORY REDUCTION
       ========================================================= */

    /**
     * Reduces inventory for multiple products in a single transaction.
     * <p>
     * Intended callers: - Order Service (after order confirmation)
     * <p>
     * This endpoint: - Locks products - Validates stock - Reduces quantities - Emits product / warehouse / analytics
     * events
     */
    @PostMapping("/reduce")
    public ResponseEntity<BulkInventoryReductionResponse> reduceInventoryBulk(
            @RequestBody BulkInventoryReductionRequest request
    ) {
        log.info("Bulk inventory reduction request received");

        productService.reduceQuantitiesBulk(request);

        BulkInventoryReductionResponse response =
                BulkInventoryReductionResponse.builder()
                        .status("SUCCESS")
                        .processedProductIds(
                                request.getItems()
                                        .stream()
                                        .map(item -> item.getProductId())
                                        .collect(Collectors.toList())
                        )
                        .processedAt(Instant.now())
                        .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PatchMapping("/{productId}/inventory/reduce")
    public ResponseEntity<Void> reduceInventory(
            @PathVariable UUID productId,
            @RequestParam int quantity
    ) {
        productService.reduceQuantity(productId, quantity);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{productId}/inventory/increase")
    public ResponseEntity<Void> increaseInventory(
            @PathVariable UUID productId,
            @RequestParam int quantity
    ) {
        productService.increaseQuantity(productId, quantity);
        return ResponseEntity.noContent().build();
    }

    /* =========================
       PRODUCT STATE
       ========================= */

    @PatchMapping("/{productId}/activate")
    public ResponseEntity<ProductResponseModel> activateProduct(
            @PathVariable UUID productId
    ) {
        return ResponseEntity.ok(productService.activateProduct(productId));
    }

    @PatchMapping("/{productId}/deactivate")
    public ResponseEntity<ProductResponseModel> deactivateProduct(
            @PathVariable UUID productId
    ) {
        return ResponseEntity.ok(productService.deactivateProduct(productId));
    }

    /* =========================
       PRICE UPDATE
       ========================= */

    @PatchMapping("/{productId}/price")
    public ResponseEntity<ProductResponseModel> updatePrice(
            @PathVariable UUID productId,
            @RequestParam BigDecimal price
    ) {
        return ResponseEntity.ok(
                productService.updatePrice(
                        productId,
                        price
                )
        );
    }

    /*
     * TODO:
     * - Add batch price lookup endpoint for Order Service
     * - Add batch inventory reservation endpoint
     * - Add pagination & sorting
     * - Add OpenAPI / Swagger annotations
     * - Add security (admin-only operations)
     */
}
