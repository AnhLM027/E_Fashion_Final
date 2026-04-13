package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;
import org.example.e_fashion.dto.request.CartVariantDTO;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
public class CartItemResponseDTO {
    private String slug;

    private String productVariantSizeId;
    private String productId;

    private String productName;
    private String productImage;

    private String colorName;
    private String sizeName;
    private Integer quantity;
    private BigDecimal price;

    private Integer availableStock;
    private Boolean outOfStock;

    private List<String> colors;
    private List<String> sizes;
    private List<CartVariantDTO> variantSizes;
}
