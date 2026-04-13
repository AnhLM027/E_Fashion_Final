package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductVariantSizeResponseDTO {

    private String id;

    private String sizeName;

    private String sku;

    private String productVariantId;

    private BigDecimal originalPrice;
    private BigDecimal salePrice;

    private Integer stock;
    private Integer reservedStock;
    private Integer availableStock;

    private Boolean isActive;
}