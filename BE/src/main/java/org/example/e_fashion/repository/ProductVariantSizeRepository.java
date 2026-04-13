package org.example.e_fashion.repository;

import org.example.e_fashion.dto.dashboard.LowStockDTO;
import org.example.e_fashion.entity.ProductVariantSizeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductVariantSizeRepository extends JpaRepository<ProductVariantSizeEntity, String> {
    List<ProductVariantSizeEntity> findByProductVariantId(String productVariantId);

    @Query("""
    SELECT new org.example.e_fashion.dto.dashboard.LowStockDTO(
        p.name,
        c.name,
        s.sizeName,
        (s.stock - s.reservedStock)
    )
    FROM ProductVariantSizeEntity s
    JOIN s.productVariant v
    JOIN v.product p
    JOIN v.color c
    WHERE (s.stock - s.reservedStock) <= :threshold
    ORDER BY (s.stock - s.reservedStock) ASC
    LIMIT 10
""")
    List<LowStockDTO> findLowStock(@Param("threshold") Integer threshold);
}
