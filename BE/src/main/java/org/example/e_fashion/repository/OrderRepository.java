package org.example.e_fashion.repository;

import org.example.e_fashion.dto.dashboard.RecentOrderDTO;
import org.example.e_fashion.dto.dashboard.RevenueByDayDTO;
import org.example.e_fashion.dto.dashboard.TopProductDTO;
import org.example.e_fashion.entity.OrderEntity;
import org.example.e_fashion.entity.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {

    List<OrderEntity> findByUser_Id(String userId);

    List<OrderEntity> findAllByOrderByCreatedAtDesc();

    List<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    @Query("""
       SELECT o FROM OrderEntity o
       WHERE (:from IS NULL OR o.createdAt >= :from)
       AND (:to IS NULL OR o.createdAt <= :to)
       ORDER BY o.createdAt DESC
    """)
    List<OrderEntity> findForExport(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // 1️⃣ Tổng doanh thu (DELIVERED + PAID)
    @Query("""
        SELECT COALESCE(SUM(o.finalPrice), 0)
        FROM OrderEntity o
        WHERE o.status = org.example.e_fashion.entity.enums.OrderStatus.DELIVERED
          AND o.paymentStatus = org.example.e_fashion.entity.enums.PaymentStatus.PAID
          AND o.createdAt BETWEEN :from AND :to
    """)
    BigDecimal sumRevenueBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // 2️⃣ Tổng số đơn theo khoảng thời gian
    @Query("""
        SELECT COUNT(o)
        FROM OrderEntity o
        WHERE o.createdAt BETWEEN :from AND :to
    """)
    long countBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT COUNT(o)
        FROM OrderEntity o
        WHERE o.status = :status
          AND o.createdAt BETWEEN :from AND :to
    """)
    long countPendingBetween(
            @Param("status") OrderStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // 2️⃣ Count by status
    long countByStatus(OrderStatus status);

    // 4️⃣ Revenue by day (JPQL chuẩn)
    @Query("""
        SELECT DATE(o.createdAt),
               COALESCE(SUM(o.finalPrice), 0)
        FROM OrderEntity o
        WHERE o.status = org.example.e_fashion.entity.enums.OrderStatus.DELIVERED
          AND o.paymentStatus = org.example.e_fashion.entity.enums.PaymentStatus.PAID
          AND o.createdAt BETWEEN :from AND :to
        GROUP BY DATE(o.createdAt)
        ORDER BY DATE(o.createdAt)
    """)
    List<Object[]> getRevenueBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // 5️⃣ Recent orders theo khoảng thời gian
    @Query("""
        SELECT new org.example.e_fashion.dto.dashboard.RecentOrderDTO(
            o.id,
            o.receiverName,
            o.finalPrice,
            o.status,
            o.createdAt
        )
        FROM OrderEntity o
        WHERE o.createdAt BETWEEN :from AND :to
        ORDER BY o.createdAt DESC
    """)
    List<RecentOrderDTO> getRecentOrdersBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    // 6️⃣ Top selling products theo khoảng thời gian
    @Query("""
        SELECT new org.example.e_fashion.dto.dashboard.TopProductDTO(
            p.id,
            p.name,
            SUM(oi.quantity)
        )
        FROM OrderItemEntity oi
        JOIN oi.order o
        JOIN oi.productVariantSize pvs
        JOIN pvs.productVariant pv
        JOIN pv.product p
        WHERE o.status = org.example.e_fashion.entity.enums.OrderStatus.DELIVERED
          AND o.paymentStatus = org.example.e_fashion.entity.enums.PaymentStatus.PAID
          AND o.createdAt BETWEEN :from AND :to
        GROUP BY p.id, p.name
        ORDER BY SUM(oi.quantity) DESC
    """)
    List<TopProductDTO> getTopSellingProductsBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
