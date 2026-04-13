package org.example.e_fashion.repository;

import org.example.e_fashion.entity.OrderStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryRepository
        extends JpaRepository<OrderStatusHistoryEntity, String> {

    List<OrderStatusHistoryEntity> findByOrder_IdOrderByCreatedAtAsc(String orderId);
}
