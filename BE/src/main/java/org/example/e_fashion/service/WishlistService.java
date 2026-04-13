package org.example.e_fashion.service;

import org.example.e_fashion.entity.WishlistEntity;

import java.util.List;

public interface WishlistService {

    void addToWishlist(String userId, String productId);

    void removeFromWishlist(String userId, String productId);

    List<WishlistEntity> getUserWishlist(String userId);

    boolean isProductInWishlist(String userId, String productId);
}