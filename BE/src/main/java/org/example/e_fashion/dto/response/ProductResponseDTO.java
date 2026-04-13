package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ProductResponseDTO {

    private String id;
    private String name;
    private String slug;
    private String description;

    private String categoryId;
    private String categoryName;
    private String categorySlug;

    private String brandId;
    private String brandName;
    private String brandSlug;

    private List<ColorResponseDTO> colors;

    private String thumbnail;

    private BigDecimal originalPrice;
    private BigDecimal salePrice;

    private Boolean isActive;
    private LocalDateTime deletedAt;
}