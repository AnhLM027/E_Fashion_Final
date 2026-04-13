package org.example.e_fashion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cart_items")
@Getter @Setter
public class CartItemEntity {

    @EmbeddedId
    private CartItemId id = new CartItemId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cartId")
    @JoinColumn(name = "cart_id")
    private CartEntity cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productVariantSizeId")
    @JoinColumn(name = "product_variant_size_id")
    private ProductVariantSizeEntity productVariantSize;

    @Column(name = "quantity")
    private Integer quantity = 1;
}