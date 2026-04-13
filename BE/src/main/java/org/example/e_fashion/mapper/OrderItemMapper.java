package org.example.e_fashion.mapper;

import org.example.e_fashion.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderItemMapper {

    public OrderItemEntity fromCartItem(
            CartItemEntity cartItem,
            OrderEntity order,
            BigDecimal price
    ) {

        ProductVariantSizeEntity variantSize = cartItem.getProductVariantSize();

        OrderItemEntity item = new OrderItemEntity();

        item.setOrder(order);
        item.setProductVariantSize(variantSize);

        // ================= SNAPSHOT =================
        item.setProductName(
                variantSize.getProductVariant()
                        .getProduct()
                        .getName()
        );

        item.setColorName(
                variantSize.getProductVariant()
                        .getColor()
                        .getName()
        );

        item.setSizeName(
                variantSize.getSizeName()
        );

        item.setQuantity(cartItem.getQuantity());
        item.setPriceAtPurchase(price);

        return item;
    }
}