package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;
import org.example.e_fashion.entity.enums.OrderStatus;
import org.example.e_fashion.entity.enums.PaymentMethod;
import org.example.e_fashion.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class OrderResponseDTO {

    private String orderId;

    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;

    private String userId;
    private String userEmail;
    private String userFullName;
    private String userPhone;
    private String userAvatar;

    private String receiverName;
    private String receiverPhone;
    private String province;
    private String district;
    private String ward;
    private String detailAddress;

    private BigDecimal totalPrice;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;

    private String couponCode;
    private String trackingNumber;

    private List<OrderItemResponseDTO> items;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}