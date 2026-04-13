package org.example.e_fashion.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class WishlistResponseDTO {

    private String productId;
    private String productName;
    private String productSlug;
    private String thumbnail;

    private BigDecimal originalPrice;
    private BigDecimal salePrice;

    private LocalDateTime createdAt;
}