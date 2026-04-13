package org.example.e_fashion.mapper;

import org.example.e_fashion.dto.response.OrderStatusHistoryResponseDTO;
import org.example.e_fashion.entity.OrderStatusHistoryEntity;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusHistoryMapper {

    public OrderStatusHistoryResponseDTO toResponse(OrderStatusHistoryEntity e) {

        OrderStatusHistoryResponseDTO dto = new OrderStatusHistoryResponseDTO();
        dto.setPreviousStatus(e.getPreviousStatus());
        dto.setNewStatus(e.getNewStatus());
        dto.setNote(e.getNote());

        dto.setChangedBy(
                e.getCreatedBy() != null ? e.getCreatedBy().getFullName() : "SYSTEM"
        );

        dto.setCreatedAt(e.getCreatedAt().toString());

        return dto;
    }
}