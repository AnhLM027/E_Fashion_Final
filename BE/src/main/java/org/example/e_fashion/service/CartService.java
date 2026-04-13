package org.example.e_fashion.service;

import org.example.e_fashion.entity.CartEntity;

public interface CartService {

    CartEntity getCart(String userId);

    CartEntity addItem(String userId, String productVariantSizeId, Integer quantity);

    CartEntity updateItem(String userId, String productVariantSizeId, Integer quantity);

    CartEntity changeVariant(String userId, String oldVariantSizeId, String newVariantSizeId);

    void removeItem(String userId, String productVariantSizeId);
}
