package org.example.e_fashion.mapper;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ProductVariantImageRequestDTO;
import org.example.e_fashion.dto.response.ProductVariantImageResponseDTO;
import org.example.e_fashion.entity.ProductVariantEntity;
import org.example.e_fashion.entity.ProductVariantImageEntity;
import org.example.e_fashion.repository.ProductVariantRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductVariantImageMapper {
    private final ProductVariantRepository productVariantRepository;

    public ProductVariantImageEntity toEntity(
            String variantId,
            ProductVariantImageRequestDTO dto
    ) {
        ProductVariantEntity productVariant = productVariantRepository
                .findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        ProductVariantImageEntity entity = new ProductVariantImageEntity();
        entity.setProductVariant(productVariant);
        entity.setImageUrl(dto.getImageUrl());
        entity.setIsPrimary(dto.getIsPrimary());
        entity.setSortOrder(dto.getSortOrder());
        return entity;
    }

    public ProductVariantImageResponseDTO toResponse(ProductVariantImageEntity entity) {
        ProductVariantImageResponseDTO dto = new ProductVariantImageResponseDTO();
        dto.setId(entity.getId());
        dto.setProductVariantId(entity.getProductVariant().getId());

        dto.setImageUrl(entity.getImageUrl());
        dto.setIsPrimary(entity.getIsPrimary());
        dto.setSortOrder(entity.getSortOrder());
        return dto;
    }
}