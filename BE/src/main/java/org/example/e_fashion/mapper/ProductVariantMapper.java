package org.example.e_fashion.mapper;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ProductVariantRequestDTO;
import org.example.e_fashion.dto.response.ProductVariantResponseDTO;
import org.example.e_fashion.entity.ColorEntity;
import org.example.e_fashion.entity.ProductEntity;
import org.example.e_fashion.entity.ProductVariantEntity;
import org.example.e_fashion.repository.ColorRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductVariantMapper {
    private final ProductVariantImageMapper productVariantImageMapper;
    private final ProductVariantSizeMapper productVariantSizeMapper;

    public ProductVariantEntity toEntity(
            ProductVariantRequestDTO dto,
            ProductEntity product,
            ColorEntity color
    ) {
        ProductVariantEntity entity = new ProductVariantEntity();

        entity.setProduct(product);
        entity.setColor(color);
        entity.setIsActive(dto.getIsActive());

        return entity;
    }

    public ProductVariantResponseDTO toResponse(ProductVariantEntity entity) {
        ProductVariantResponseDTO dto = new ProductVariantResponseDTO();

        dto.setId(entity.getId());
        dto.setIsActive(entity.getIsActive());

        dto.setColorId(entity.getColor().getId());
        dto.setColorName(entity.getColor().getName());
        dto.setColorCode(entity.getColor().getCode());

        dto.setProductId(entity.getProduct().getId());
        dto.setProductName(entity.getProduct().getName());

        if (entity.getImages() != null) {
            dto.setImages(
                    entity.getImages().stream()
                            .map(productVariantImageMapper::toResponse)
                            .toList()
            );
        }

        if (entity.getSizes() != null) {
            dto.setSizes(
                    entity.getSizes().stream()
                            .map(productVariantSizeMapper::toResponse)
                            .toList()
            );
        }

        return dto;
    }

    public void updateEntity(ProductVariantEntity entity, ProductVariantRequestDTO dto, ColorEntity colorEntity) {

        if (dto.getIsActive() != null) entity.setIsActive(dto.getIsActive());

        if (dto.getColorId() != null) entity.setColor(colorEntity);
    }
}