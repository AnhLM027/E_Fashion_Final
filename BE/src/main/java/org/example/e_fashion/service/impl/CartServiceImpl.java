package org.example.e_fashion.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.entity.*;
import org.example.e_fashion.repository.*;
import org.example.e_fashion.service.CartService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductVariantSizeRepository variantSizeRepository;

    private CartEntity getOrCreateCart(String userId) {

        return cartRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    UserEntity user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    CartEntity cart = new CartEntity();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    @Override
    public CartEntity getCart(String userId) {
        return getOrCreateCart(userId);
    }

    @Override
    public CartEntity addItem(String userId, String variantSizeId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        CartEntity cart = getOrCreateCart(userId);

        ProductVariantSizeEntity variantSize = variantSizeRepository.findById(variantSizeId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        int availableStock =
                variantSize.getStock() - variantSize.getReservedStock();

        CartItemId id = new CartItemId(cart.getId(), variantSizeId);

        CartItemEntity item = cartItemRepository.findById(id).orElse(null);

        int newQuantity = quantity;

        if (item != null) {
            newQuantity = item.getQuantity() + quantity;
        }

        if (newQuantity > availableStock) {
            throw new RuntimeException(
                    "Not enough stock available. Only " + availableStock + " left."
            );
        }

        if (item != null) {
            item.setQuantity(newQuantity);
        } else {
            CartItemEntity newItem = new CartItemEntity();
            newItem.setId(id);
            newItem.setCart(cart);
            newItem.setProductVariantSize(variantSize);
            newItem.setQuantity(quantity);

            cart.getItems().add(newItem);
        }

        return cartRepository.save(cart);
    }

    @Override
    public CartEntity updateItem(String userId,
                                 String productVariantSizeId,
                                 Integer quantity) {

        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        CartEntity cart = getOrCreateCart(userId);

        CartItemId id = new CartItemId(cart.getId(), productVariantSizeId);
        CartItemEntity item = cartItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        ProductVariantSizeEntity variantSize =
                item.getProductVariantSize();

        int availableStock =
                variantSize.getStock() - variantSize.getReservedStock();

        if (quantity > availableStock) {
            throw new RuntimeException(
                    "Not enough stock available. Only " + availableStock + " left."
            );
        }

        item.setQuantity(quantity);

        return cart;
    }

    @Override
    @Transactional
    public CartEntity changeVariant(String userId, String oldVariantSizeId, String newVariantSizeId) {
        CartEntity cart = getCart(userId);

        CartItemEntity oldItem = cartItemRepository
                .findByCartIdAndProductVariantSize_Id(cart.getId(), oldVariantSizeId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        Optional<CartItemEntity> existingNewItem =
                cartItemRepository.findByCartIdAndProductVariantSize_Id(
                        cart.getId(),
                        newVariantSizeId
                );

        if (existingNewItem.isPresent()) {
            CartItemEntity newItem = existingNewItem.get();
            newItem.setQuantity(newItem.getQuantity() + oldItem.getQuantity());
            cartItemRepository.save(newItem);
            cart.getItems().remove(oldItem);
            cartItemRepository.delete(oldItem);
        } else {
            ProductVariantSizeEntity newVariant =
                    variantSizeRepository.findById(newVariantSizeId)
                            .orElseThrow(() -> new RuntimeException("Variant not found"));

            if (newVariant.getStock() < oldItem.getQuantity()) {
                throw new RuntimeException("Not enough stock");
            }

            CartItemEntity newItem = new CartItemEntity();

            CartItemId newId = new CartItemId();
            newId.setCartId(cart.getId());
            newId.setProductVariantSizeId(newVariantSizeId);

            newItem.setId(newId);
            newItem.setCart(cart);
            newItem.setProductVariantSize(newVariant);
            newItem.setQuantity(oldItem.getQuantity());

            cart.getItems().remove(oldItem);
            cartItemRepository.delete(oldItem);

            cart.getItems().remove(oldItem);
            cartItemRepository.save(newItem);
        }

        return cartRepository.findById(cart.getId()).orElseThrow();
    }

    @Override
    public void removeItem(String userId, String variantSizeId) {

        CartEntity cart = getOrCreateCart(userId);
        CartItemEntity item = cart.getItems().stream()
                .filter(i -> i.getProductVariantSize().getId().equals(variantSizeId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found"));

        cart.getItems().remove(item);
    }
}
