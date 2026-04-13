package org.example.e_fashion.repository;

import org.example.e_fashion.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, String> {
    @Query("""
        SELECT oi
        FROM OrderItemEntity oi
        JOIN oi.order o
        WHERE oi.id = :orderItemId
        AND o.user.id = :userId
        AND o.status = 'DELIVERED'
    """)
    Optional<OrderItemEntity> findDeliveredOrderItemByUser(String userId, String orderItemId);
}