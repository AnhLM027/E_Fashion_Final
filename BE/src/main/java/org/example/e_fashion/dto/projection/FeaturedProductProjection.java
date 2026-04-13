package org.example.e_fashion.dto.projection;

import java.math.BigDecimal;

public interface FeaturedProductProjection {

    String getProductId();
    String getName();
    String getSlug();
    String getThumbnail();
    BigDecimal getMinPrice();
    BigDecimal getMaxPrice();
    Integer getIsOnSale();
    Long getTotalSold();
}