package org.example.e_fashion.repository;

import org.example.e_fashion.entity.ProductVariantImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVariantImageRepository
        extends JpaRepository<ProductVariantImageEntity, String> {
    Optional<ProductVariantImageEntity> findFirstByProductVariantIdAndIsPrimaryTrue(String variantId);

    List<ProductVariantImageEntity> findByProductVariantIdOrderBySortOrderAsc(String productId);

    @Modifying
    @Query("""
    UPDATE ProductVariantImageEntity PI
    SET PI.isPrimary = false
    WHERE PI.productVariant.id = :productId
""")
    void clearPrimaryByProductVariantId(@Param("productId") String productId);

    @Query("""
    SELECT MAX(PI.sortOrder)
    FROM ProductVariantImageEntity PI
    WHERE PI.productVariant.id = :productId
""")
    Integer findMaxSortOrderByProductVariantId(@Param("productId") String productId);
}