package org.example.e_fashion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItemEntity {

    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_size_id", nullable = false)
    private ProductVariantSizeEntity productVariantSize;

    @Column(name = "product_name", length = 255, nullable = false)
    private String productName;

    @Column(name = "color_name", length = 100, nullable = false)
    private String colorName;

    @Column(name = "size_name", length = 50, nullable = false)
    private String sizeName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "price_at_purchase", nullable = false)
    private BigDecimal priceAtPurchase;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}