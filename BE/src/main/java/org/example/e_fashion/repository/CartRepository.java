package org.example.e_fashion.repository;

import org.example.e_fashion.entity.CartEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<CartEntity, String> {
    @EntityGraph(attributePaths = {
            "items",
            "items.productVariantSize",
            "items.productVariantSize.productVariant",
            "items.productVariantSize.productVariant.product",
            "items.productVariantSize.productVariant.color"
    })
    Optional<CartEntity> findByUser_Id(String userId);
}
