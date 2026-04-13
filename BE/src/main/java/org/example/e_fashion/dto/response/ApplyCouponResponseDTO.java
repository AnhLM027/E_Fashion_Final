package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ApplyCouponResponseDTO {

    private Boolean applicable;
    private String message;

    private BigDecimal discountAmount;
    private BigDecimal finalTotal;
}