package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;
import org.example.e_fashion.entity.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CouponResponseDTO {

    private String id;
    private String code;

    private BigDecimal discountValue;
    private DiscountType discountType;

    private BigDecimal minOrderValue;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Integer usageLimit;
    private Boolean isActive;

    // client info
    private Boolean isUsed;
    private Boolean applicable;
    private String message;
}