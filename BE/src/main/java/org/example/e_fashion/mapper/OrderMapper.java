package org.example.e_fashion.mapper;

import org.example.e_fashion.dto.request.OrderCreateRequestDTO;
import org.example.e_fashion.dto.response.OrderItemResponseDTO;
import org.example.e_fashion.dto.response.OrderResponseDTO;
import org.example.e_fashion.entity.OrderEntity;
import org.example.e_fashion.entity.ProductVariantImageEntity;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.entity.enums.OrderStatus;
import org.example.e_fashion.entity.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderMapper {

    public OrderResponseDTO toResponse(OrderEntity order) {

        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setOrderId(order.getId());
        dto.setStatus(order.getStatus());

        UserEntity user = order.getUser();
        if (user != null) {
            dto.setUserId(user.getId());
            dto.setUserEmail(user.getEmail());
            dto.setUserFullName(user.getFullName());
            dto.setUserPhone(user.getPhone());
            dto.setUserAvatar(user.getAvatarUrl());
        }

        dto.setReceiverName(order.getReceiverName());
        dto.setReceiverPhone(order.getReceiverPhone());
        dto.setProvince(order.getProvince());
        dto.setDistrict(order.getDistrict());
        dto.setWard(order.getWard());
        dto.setDetailAddress(order.getDetailAddress());

        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentStatus(order.getPaymentStatus());

        dto.setTotalPrice(order.getTotalPrice());
        dto.setShippingFee(order.getShippingFee());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setFinalPrice(order.getFinalPrice());
        dto.setCouponCode(order.getCouponCode());
        dto.setTrackingNumber(order.getTrackingNumber());

        dto.setItems(
                order.getItems().stream().map(item -> {
                    OrderItemResponseDTO i = new OrderItemResponseDTO();

                    var variantSize = item.getProductVariantSize();
                    var variant = variantSize.getProductVariant();
                    var product = variant.getProduct();

                    i.setOrderItemId(item.getId());
                    i.setSlug(product.getSlug());

                    i.setProductId(product.getId());
                    i.setVariantId(variant.getId());
                    i.setVariantSizeId(variantSize.getId());

                    i.setProductName(product.getName());
                    i.setColorName(variant.getColor().getName());
                    i.setSizeName(variantSize.getSizeName());

                    i.setCurrentStock(variantSize.getStock());

                    if (variant.getImages() != null && !variant.getImages().isEmpty()) {
                        i.setImageUrl(
                                variant.getImages().stream().min((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                                        .map(ProductVariantImageEntity::getImageUrl)
                                        .orElse(null)
                        );
                    }

                    i.setQuantity(item.getQuantity());
                    i.setPrice(item.getPriceAtPurchase());

                    i.setSubtotal(
                            item.getPriceAtPurchase()
                                    .multiply(java.math.BigDecimal.valueOf(item.getQuantity()))
                    );
                    return i;
                }).toList()
        );

        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        return dto;
    }

    public OrderEntity toEntity(OrderCreateRequestDTO requestDTO) {

        OrderEntity order = new OrderEntity();

        order.setReceiverName(requestDTO.getReceiverName());
        order.setReceiverPhone(requestDTO.getReceiverPhone());
        order.setProvince(requestDTO.getProvince());
        order.setDistrict(requestDTO.getDistrict());
        order.setWard(requestDTO.getWard());
        order.setDetailAddress(requestDTO.getDetailAddress());

        order.setPaymentMethod(requestDTO.getPaymentMethod());

        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.UNPAID);

        order.setShippingFee(
                requestDTO.getShippingFee() != null
                        ? requestDTO.getShippingFee()
                        : BigDecimal.ZERO
        );

        return order;
    }
}