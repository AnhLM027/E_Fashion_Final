package org.example.e_fashion.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ApplyCouponRequestDTO {

    private String couponCode;
    private BigDecimal orderTotal;
}