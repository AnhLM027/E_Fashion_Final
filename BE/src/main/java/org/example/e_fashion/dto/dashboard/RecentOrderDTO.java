package org.example.e_fashion.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.e_fashion.entity.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RecentOrderDTO {

    private String id;
    private String receiverName;
    private BigDecimal finalPrice;
    private OrderStatus status;
    private LocalDateTime createdAt;
}