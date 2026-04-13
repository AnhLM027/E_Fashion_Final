package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RatingResponseDTO {

    private String id;

    private String userId;
    private String userName;

    private String productId;

    private String orderItemId;

    private Integer rating;

    private String reviewText;

    private LocalDateTime createdAt;

    private RatingOrderItemDTO orderItem;
}