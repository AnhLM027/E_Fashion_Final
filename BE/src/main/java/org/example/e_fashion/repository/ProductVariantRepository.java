package org.example.e_fashion.repository;

import org.example.e_fashion.entity.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, String> {

    List<ProductVariantEntity> findByProduct_Id(String productId);

    List<ProductVariantEntity> findByProductIdAndIsActiveTrue(String productId);

    long countByProduct_IdAndIsActiveTrue(String id);
}