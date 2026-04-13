package org.example.e_fashion.repository;

import org.example.e_fashion.entity.WishlistEntity;
import org.example.e_fashion.entity.WishlistId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WishlistRepository extends JpaRepository<WishlistEntity, WishlistId> {

    List<WishlistEntity> findByUser_Id(String userId);

    boolean existsByUser_IdAndProduct_Id(String userId, String productId);

    void deleteByUser_IdAndProduct_Id(String userId, String productId);
}