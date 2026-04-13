package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderItemResponseDTO {
    private String orderItemId;
    private String slug;

    private String productId;
    private String productName;

    private String variantId;
    private String variantSizeId;

    private String imageUrl;
    private String colorName;
    private String sizeName;

    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;

    private Integer currentStock;
}
