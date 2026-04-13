package org.example.e_fashion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter @Setter
@Table(name = "product_variant_sizes")
public class ProductVariantSizeEntity {
    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariantEntity productVariant;

    @Column(name = "sku", nullable = false, unique = true)
    private String sku;

    @Column(name = "size_name", nullable = false)
    private String sizeName;

    @Column(name = "original_price")
    private BigDecimal originalPrice = BigDecimal.ZERO;

    @Column(name = "sale_price")
    private BigDecimal salePrice = BigDecimal.ZERO;

    @Column(name = "stock")
    private Integer stock = 0;

    @Column(name = "reserved_stock")
    private Integer reservedStock = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}
