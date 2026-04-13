package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusHistoryResponseDTO {

    private String previousStatus;
    private String newStatus;
    private String note;
    private String changedBy;
    private String createdAt;
}