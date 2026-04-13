package org.example.e_fashion.mapper;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.response.ProductResponseDTO;
import org.example.e_fashion.dto.response.WishlistResponseDTO;
import org.example.e_fashion.entity.WishlistEntity;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class WishlistMapper {

    private final ProductMapper productMapper;

    public WishlistResponseDTO toResponse(WishlistEntity entity) {

        ProductResponseDTO productDTO =
                productMapper.toResponse(entity.getProduct());

        return WishlistResponseDTO.builder()
                .productId(productDTO.getId())
                .productName(productDTO.getName())
                .productSlug(productDTO.getSlug())
                .thumbnail(productDTO.getThumbnail())
                .originalPrice(productDTO.getOriginalPrice())
                .salePrice(productDTO.getSalePrice())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}