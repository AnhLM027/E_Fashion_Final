package org.example.e_fashion.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductVariantRequestDTO {
    private String productId;
    private String colorId;
    private String sku;
    private Boolean isActive;
}