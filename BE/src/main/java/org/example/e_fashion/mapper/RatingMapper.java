package org.example.e_fashion.mapper;

import org.example.e_fashion.dto.response.RatingOrderItemDTO;
import org.example.e_fashion.dto.response.RatingResponseDTO;
import org.example.e_fashion.entity.ProductVariantImageEntity;
import org.example.e_fashion.entity.RatingEntity;
import org.springframework.stereotype.Component;

@Component
public class RatingMapper {

    public RatingResponseDTO toResponse(RatingEntity entity) {

        RatingResponseDTO dto = new RatingResponseDTO();

        dto.setId(entity.getId());
        if (entity.getProduct() != null) {
            dto.setProductId(entity.getProduct().getId());
        }
        if (entity.getOrderItem() != null) {
            dto.setOrderItemId(entity.getOrderItem().getId());

            RatingOrderItemDTO orderItemDTO = new RatingOrderItemDTO();

            if (entity.getOrderItem().getProductVariantSize() != null) {
                orderItemDTO.setSizeName(
                        entity.getOrderItem().getSizeName()
                );

                orderItemDTO.setColorName(
                        entity.getOrderItem().getColorName()
                );

                orderItemDTO.setProductName(
                        entity.getOrderItem().getProductName()
                );

                orderItemDTO.setImageUrl(
                        entity.getOrderItem()
                                .getProductVariantSize()
                                .getProductVariant()
                                .getImages()
                                .stream()
                                .findFirst()
                                .map(ProductVariantImageEntity::getImageUrl)
                                .orElse(null)
                );
            }

            dto.setOrderItem(orderItemDTO);
        }

        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setUserName(entity.getUser().getFullName());
        }

        dto.setRating(entity.getRating());
        dto.setReviewText(entity.getReviewText());

        dto.setCreatedAt(entity.getCreatedAt());

        return dto;
    }
}