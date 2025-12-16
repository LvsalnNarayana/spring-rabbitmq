package com.example.product_service.services;

import com.example.product_service.entity.ProductEntity;
import com.example.product_service.models.BulkInventoryReductionRequest;
import com.example.product_service.models.CreateProductRequestModel;
import com.example.product_service.models.ProductResponseModel;
import com.example.product_service.repository.ProductRepository;
import com.example.product_service.utils.RabbitMQSender;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductService {

    private static final String DEFAULT_WAREHOUSE_ID = "BLRA";

    private final ProductRepository productRepository;
    private final RabbitMQSender rabbitMQSender;

    /* =========================
       CREATE
       ========================= */

    @Transactional
    public ProductResponseModel createProduct(CreateProductRequestModel request) {

        ProductEntity product = ProductEntity.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .availableQuantity(request.getAvailableQuantity())
                .sku(request.getSku())
                .category(request.getCategory())
                .brand(request.getBrand())
                .status(ProductEntity.ProductStatus.ACTIVE)
                .build();

        ProductEntity saved = productRepository.save(product);

        Map<String, Object> event = baseEvent(saved);
        event.put("eventType", "PRODUCT_CREATED");

        // 📊 Analytics fanout
        rabbitMQSender.sendEventToAnalytics(event);

        log.info("Product created. productId={}", saved.getId());
        return mapToResponse(saved);
    }

    /* =========================
       READ
       ========================= */

    public ProductResponseModel getProductById(UUID productId) {
        return mapToResponse(getProductEntity(productId));
    }


    public List<ProductResponseModel> getProductsByIdsStrict(List<UUID> productIds) {

        List<ProductEntity> products = productRepository.findByIdIn(productIds);

        if (products.size() != productIds.size()) {
            throw new EntityNotFoundException(
                    "One or more products not found for ids: " + productIds
            );
        }

        return products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponseModel> getProductsByStatus(ProductEntity.ProductStatus status) {
        return productRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponseModel> getProductsByCategory(String category) {
        return productRepository.findByCategory(category)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /* =========================
       INVENTORY OPERATIONS
       ========================= */

    @Transactional
    public void reduceQuantity(UUID productId, int quantity) {

        ProductEntity product = getActiveProduct(productId);

        if (product.getAvailableQuantity() < quantity) {
            throw new IllegalStateException("Insufficient stock for product: " + productId);
        }

        product.setAvailableQuantity(product.getAvailableQuantity() - quantity);

        boolean outOfStock = product.getAvailableQuantity() == 0;
        if (outOfStock) {
            product.setStatus(ProductEntity.ProductStatus.OUT_OF_STOCK);
        }

        ProductEntity saved = productRepository.save(product);

        Map<String, Object> event = baseEvent(saved);
        event.put("eventType", "INVENTORY_REDUCED");
        event.put("quantityReduced", quantity);

        // 📊 Analytics
        rabbitMQSender.sendEventToAnalytics(event);

        // 🏭 Warehouse must know if product is OUT_OF_STOCK
        if (outOfStock) {
            Map<String, Object> warehouseEvent = new HashMap<>(event);
            warehouseEvent.put("eventType", "OUT_OF_STOCK");

            rabbitMQSender.sendInventoryUpdateToWarehouse(
                    DEFAULT_WAREHOUSE_ID,
                    warehouseEvent
            );
        }

        log.info("Inventory reduced. productId={}, remaining={}",
                productId, saved.getAvailableQuantity());
    }

    @Transactional
    public void increaseQuantity(UUID productId, int quantity) {

        ProductEntity product = getActiveOrOutOfStockProduct(productId);

        product.setAvailableQuantity(product.getAvailableQuantity() + quantity);

        boolean backInStock =
                product.getStatus() == ProductEntity.ProductStatus.OUT_OF_STOCK
                        && product.getAvailableQuantity() > 0;

        if (backInStock) {
            product.setStatus(ProductEntity.ProductStatus.ACTIVE);
        }

        ProductEntity saved = productRepository.save(product);

        Map<String, Object> event = baseEvent(saved);
        event.put("eventType", "INVENTORY_INCREASED");
        event.put("quantityAdded", quantity);

        // 📊 Analytics
        rabbitMQSender.sendEventToAnalytics(event);

        if (backInStock) {
            Map<String, Object> backInStockEvent = new HashMap<>(event);
            backInStockEvent.put("eventType", "PRODUCT_BACK_IN_STOCK");

            // ⏳ Delayed user notification (TTL)
            rabbitMQSender.sendDelayedUserNotification(backInStockEvent);
        }

        log.info("Inventory increased. productId={}, available={}",
                productId, saved.getAvailableQuantity());
    }


    @Transactional
    public void reduceQuantitiesBulk(
            BulkInventoryReductionRequest request
    ) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Inventory reduction request is empty");
        }

        // 1️⃣ Collect product IDs
        List<UUID> productIds = request.getItems()
                .stream()
                .map(BulkInventoryReductionRequest.Item::getProductId)
                .toList();

        // 2️⃣ Lock all products (prevents overselling)
        List<ProductEntity> products =
                productRepository.findAllByIdInForUpdate(productIds);

        if (products.size() != productIds.size()) {
            throw new EntityNotFoundException(
                    "One or more products not found for ids: " + productIds
            );
        }

        // 3️⃣ Index products
        Map<UUID, ProductEntity> productMap =
                products.stream()
                        .collect(Collectors.toMap(ProductEntity::getId, p -> p));

        // 4️⃣ Validate stock
        for (BulkInventoryReductionRequest.Item item : request.getItems()) {

            ProductEntity product = productMap.get(item.getProductId());

            if (product.getStatus() != ProductEntity.ProductStatus.ACTIVE) {
                throw new IllegalStateException(
                        "Product is not active: " + product.getId()
                );
            }

            if (product.getAvailableQuantity() < item.getQuantity()) {
                throw new IllegalStateException(
                        "Insufficient stock for product: " + product.getId()
                );
            }
        }

        // 5️⃣ Reduce quantities
        for (BulkInventoryReductionRequest.Item item : request.getItems()) {

            ProductEntity product = productMap.get(item.getProductId());

            product.setAvailableQuantity(
                    product.getAvailableQuantity() - item.getQuantity()
            );

            boolean outOfStock = product.getAvailableQuantity() == 0;
            if (outOfStock) {
                product.setStatus(ProductEntity.ProductStatus.OUT_OF_STOCK);
            }

            ProductEntity saved = productRepository.save(product);

            // 📊 Analytics event
            Map<String, Object> event = baseEvent(saved);
            event.put("eventType", "INVENTORY_REDUCED");
            event.put("quantityReduced", item.getQuantity());

            rabbitMQSender.sendEventToAnalytics(event);

            // 🏭 Warehouse notification if OUT_OF_STOCK
            if (outOfStock) {
                Map<String, Object> warehouseEvent = new HashMap<>(event);
                warehouseEvent.put("eventType", "OUT_OF_STOCK");

                rabbitMQSender.sendInventoryUpdateToWarehouse(
                        DEFAULT_WAREHOUSE_ID,
                        warehouseEvent
                );
            }
        }

        log.info(
                "Bulk inventory reduction completed for products: {}",
                productIds
        );
    }

    /* =========================
       PRODUCT STATE OPERATIONS
       ========================= */

    @Transactional
    public ProductResponseModel deactivateProduct(UUID productId) {

        ProductEntity product = getProductEntity(productId);
        product.setStatus(ProductEntity.ProductStatus.INACTIVE);

        ProductEntity saved = productRepository.save(product);

        Map<String, Object> event = baseEvent(saved);
        event.put("eventType", "PRODUCT_DEACTIVATED");

        rabbitMQSender.sendEventToAnalytics(event);

        return mapToResponse(saved);
    }

    @Transactional
    public ProductResponseModel activateProduct(UUID productId) {

        ProductEntity product = getProductEntity(productId);
        product.setStatus(ProductEntity.ProductStatus.ACTIVE);

        ProductEntity saved = productRepository.save(product);

        Map<String, Object> event = baseEvent(saved);
        event.put("eventType", "PRODUCT_ACTIVATED");

        rabbitMQSender.sendEventToAnalytics(event);

        return mapToResponse(saved);
    }

    /* =========================
       PRICE OPERATIONS
       ========================= */

    @Transactional
    public ProductResponseModel updatePrice(UUID productId, BigDecimal newPrice) {

        if (newPrice.signum() <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }

        ProductEntity product = getProductEntity(productId);
        product.setPrice(newPrice);

        ProductEntity saved = productRepository.save(product);

        Map<String, Object> event = baseEvent(saved);
        event.put("eventType", "PRICE_UPDATED");
        event.put("newPrice", newPrice);

        rabbitMQSender.sendEventToAnalytics(event);

        return mapToResponse(saved);
    }

    /* =========================
       EVENT BASE
       ========================= */

    private Map<String, Object> baseEvent(ProductEntity product) {
        Map<String, Object> event = new HashMap<>();
        event.put("productId", product.getId());
        event.put("sku", product.getSku());
        event.put("status", product.getStatus().name());
        event.put("availableQuantity", product.getAvailableQuantity());
        event.put("timestamp", System.currentTimeMillis());
        return event;
    }

    /* =========================
       HELPERS
       ========================= */

    private ProductEntity getProductEntity(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Product not found: " + productId));
    }

    private ProductEntity getActiveProduct(UUID productId) {
        ProductEntity product = getProductEntity(productId);
        if (product.getStatus() != ProductEntity.ProductStatus.ACTIVE) {
            throw new IllegalStateException("Product is not active: " + productId);
        }
        return product;
    }

    private ProductEntity getActiveOrOutOfStockProduct(UUID productId) {
        ProductEntity product = getProductEntity(productId);
        if (product.getStatus() == ProductEntity.ProductStatus.DISCONTINUED) {
            throw new IllegalStateException("Product is discontinued: " + productId);
        }
        return product;
    }

    /* =========================
       MAPPING
       ========================= */

    private ProductResponseModel mapToResponse(ProductEntity product) {
        return ProductResponseModel.builder()
                .id(product.getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .availableQuantity(product.getAvailableQuantity())
                .sku(product.getSku())
                .category(product.getCategory())
                .brand(product.getBrand())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
