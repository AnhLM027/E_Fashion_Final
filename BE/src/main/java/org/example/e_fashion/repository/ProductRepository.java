package org.example.e_fashion.repository;

import org.example.e_fashion.entity.ProductEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, String> {

    boolean existsBySlug(String slug);

    @Query("""
    SELECT p FROM ProductEntity p
    WHERE p.isActive = true
    AND p.deletedAt IS NULL
    AND EXISTS (
        SELECT v FROM ProductVariantEntity v
        WHERE v.product.id = p.id
        AND v.isActive = true
    )
""")
    List<ProductEntity> findVisibleProducts();

    Optional<ProductEntity> findByIdAndDeletedAtIsNull(String id);

    List<ProductEntity> findByCategory_IdInAndIsActiveTrueAndDeletedAtIsNull(List<String> categoryIds);

    Optional<ProductEntity> findBySlugAndDeletedAtIsNull(String slug);

    List<ProductEntity> findByCategory_IdIn(List<String> categoryIds);

    @Query("""
    SELECT DISTINCT p
    FROM ProductEntity p
    JOIN p.variants v
    JOIN v.sizes s
    JOIN v.color c
    WHERE p.deletedAt IS NULL
      AND p.isActive = true
      AND v.deletedAt IS NULL
      AND v.isActive = true
      AND s.stock > 0
      AND (:categoryIds IS NULL OR p.category.id IN :categoryIds)
      AND (:brandIds IS NULL OR p.brand.id IN :brandIds)
      AND (:colorIds IS NULL OR c.id IN :colorIds)
      AND s.salePrice BETWEEN :minPrice AND :maxPrice
""")
    List<ProductEntity> filterProducts(
            @Param("categoryIds") List<String> categoryIds,
            @Param("brandIds") List<String> brandIds,
            @Param("colorIds") List<String> colorIds,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );

    @Query("""
    SELECT DISTINCT p FROM ProductEntity p
    LEFT JOIN p.brand b
    LEFT JOIN p.category c
    WHERE p.deletedAt IS NULL
    AND p.isActive = true
    AND (
        LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
""")
    List<ProductEntity> search(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
    SELECT p FROM ProductEntity p
    WHERE (:categoryIds IS NULL OR p.category.id IN :categoryIds)
    AND (:brandId IS NULL OR p.brand.id = :brandId)
    AND (:isActive IS NULL OR p.isActive = :isActive)
    AND p.deletedAt IS NULL
    ORDER BY p.createdAt DESC
""")
    List<ProductEntity> getProductsForStaff(
            @Param("categoryIds") List<String> categoryIds,
            @Param("brandId") String brandId,
            @Param("isActive") Boolean isActive
    );
}