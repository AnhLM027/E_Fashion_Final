package org.example.e_fashion.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CartItemId implements Serializable {

    @Column(name = "cart_id", length = 36)
    private String cartId;

    @Column(name = "product_variant_size_id", length = 36)
    private String productVariantSizeId;
}