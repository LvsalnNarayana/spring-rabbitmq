package com.example.product_service.repository;

import com.example.product_service.entity.ProductEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    Optional<ProductEntity> findBySku(String sku);

    List<ProductEntity> findByStatus(ProductEntity.ProductStatus status);

    List<ProductEntity> findByCategory(String category);

    List<ProductEntity> findByIdIn(List<UUID> ids);

    boolean existsBySku(String sku);
/* =========================================================
       INVENTORY-SAFE OPERATIONS
       ========================================================= */

    /**
     * Locks products during order placement Prevents concurrent overselling
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT p
                FROM ProductEntity p
                WHERE p.id IN :ids
            """)
    List<ProductEntity> findAllByIdInForUpdate(List<UUID> ids);

    /**
     * Atomically reduce quantity for a single product Returns number of rows updated (0 or 1)
     */
    @Modifying
    @Query("""
                UPDATE ProductEntity p
                SET p.availableQuantity = p.availableQuantity - :quantity
                WHERE p.id = :productId
                  AND p.availableQuantity >= :quantity
            """)
    int reduceQuantity(UUID productId, int quantity);
    /*
     * TODO:
     * - Add pessimistic locking for inventory reservation
     * - Add custom JPQL for batch price lookup
     * - Add projections for lightweight price queries
     */
}
