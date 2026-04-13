package org.example.e_fashion.repository;

import org.example.e_fashion.entity.ProductAttributeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductAttributeRepository
        extends JpaRepository<ProductAttributeEntity, String> {

    List<ProductAttributeEntity> findByProduct_Id(String productId);

    boolean existsByProduct_IdAndAttributeName(String productId, String attributeName);
}