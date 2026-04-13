package org.example.e_fashion.service;

import org.example.e_fashion.dto.request.ChangeOrderStatusRequestDTO;
import org.example.e_fashion.entity.OrderStatusHistoryEntity;

import java.util.Collection;
import java.util.List;

public interface OrderStatusHistoryService {
    void changeOrderStatus(
            String orderId,
            ChangeOrderStatusRequestDTO request,
            String adminId
    );

    List<OrderStatusHistoryEntity> getHistoryByOrder(String orderId);

    List<OrderStatusHistoryEntity> getHistoryByOrderAndUser(String orderId, String userId);
}