package org.example.e_fashion.repository;

import org.example.e_fashion.entity.RatingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<RatingEntity, String> {

    List<RatingEntity> findByProduct_Id(String productId);

    Optional<RatingEntity> findByIdAndUserId(String ratingId, String userId);

    boolean existsByOrderItem_Id(String orderItemId);

    Page<RatingEntity> findByProduct_IdOrderByCreatedAtDesc(String productId, Pageable pageable);

    List<RatingEntity> findByOrderItem_Order_Id(String orderId);

    @Query("""
        SELECT AVG(r.rating)
        FROM RatingEntity r
        WHERE r.product.id = :productId
    """)
    Double getAverageRatingByProduct(String productId);

    long countByProduct_Id(String productId);
}