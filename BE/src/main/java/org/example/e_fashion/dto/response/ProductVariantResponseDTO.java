package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductVariantResponseDTO {

    private String id;

    private Boolean isActive;

    private String productId;
    private String productName;

    private String colorId;
    private String colorName;
    private String colorCode;

    private List<ProductVariantImageResponseDTO> images;
    private List<ProductVariantSizeResponseDTO> sizes;
}