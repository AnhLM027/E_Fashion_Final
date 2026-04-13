package org.example.e_fashion.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.example.e_fashion.entity.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CouponRequestDTO {

    private String code;
    private BigDecimal discountValue;
    private DiscountType discountType;

    private BigDecimal minOrderValue;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Integer usageLimit;
    private Boolean isActive;
}