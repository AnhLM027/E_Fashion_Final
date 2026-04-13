package org.example.e_fashion.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FeaturedProductResponseDTO {

    private String productId;
    private String name;
    private String slug;

    private String thumbnail;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private Integer isOnSale;

    private Long totalSold; // null nếu không phải best seller
}