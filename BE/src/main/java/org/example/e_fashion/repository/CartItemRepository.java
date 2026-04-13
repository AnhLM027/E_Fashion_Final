package org.example.e_fashion.repository;

import org.example.e_fashion.entity.CartItemEntity;
import org.example.e_fashion.entity.CartItemId;
import org.example.e_fashion.entity.ProductVariantSizeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItemEntity, CartItemId> {
    Optional<CartItemEntity> findByCartIdAndProductVariantSize_Id(String cartId, String productVariantSizeId);
}
