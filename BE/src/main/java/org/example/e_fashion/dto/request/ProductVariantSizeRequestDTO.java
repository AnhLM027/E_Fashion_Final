package org.example.e_fashion.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class ProductVariantSizeRequestDTO {
    private String productVariantId;
    private String sizeName;

    private String sku;

    private BigDecimal originalPrice = BigDecimal.ZERO;
    private BigDecimal salePrice = BigDecimal.ZERO;

    private Integer stock = 0;
    private Integer reserved_stock = 0;
}
