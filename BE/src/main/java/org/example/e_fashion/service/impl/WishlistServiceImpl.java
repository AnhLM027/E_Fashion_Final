package org.example.e_fashion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.entity.ProductEntity;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.entity.WishlistEntity;
import org.example.e_fashion.entity.WishlistId;
import org.example.e_fashion.mapper.WishlistMapper;
import org.example.e_fashion.repository.ProductRepository;
import org.example.e_fashion.repository.UserRepository;
import org.example.e_fashion.repository.WishlistRepository;
import org.example.e_fashion.service.WishlistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WishlistMapper wishlistMapper;

    @Override
    public void addToWishlist(String userId, String productId) {

        if (wishlistRepository.existsByUser_IdAndProduct_Id(userId, productId)) {
            return; // đã tồn tại thì bỏ qua
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        WishlistEntity wishlist = new WishlistEntity();
        wishlist.setUser(user);
        wishlist.setProduct(product);

        wishlistRepository.save(wishlist);
    }

    @Override
    public void removeFromWishlist(String userId, String productId) {
        wishlistRepository.deleteByUser_IdAndProduct_Id(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistEntity> getUserWishlist(String userId) {

        return wishlistRepository.findByUser_Id(userId);
    }

    @Override
    public boolean isProductInWishlist(String userId, String productId) {
        return wishlistRepository.existsByUser_IdAndProduct_Id(userId, productId);
    }
}