package org.example.e_fashion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.example.e_fashion.entity.enums.PaymentMethod;

import java.math.BigDecimal;

@Getter @Setter
public class OrderCreateRequestDTO {
    @NotBlank(message = "Receiver name is required")
    private String receiverName;

    @NotBlank(message = "Receiver phone is required")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$",
            message = "Phone number is invalid")
    private String receiverPhone;

    @NotBlank(message = "Province is required")
    private String province;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Ward is required")
    private String ward;

    @NotBlank(message = "Detail address is required")
    private String detailAddress;

    @NotNull(message = "Shipping fee is required")
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String couponCode;
}
