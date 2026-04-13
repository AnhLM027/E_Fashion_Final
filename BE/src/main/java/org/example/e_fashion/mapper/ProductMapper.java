package org.example.e_fashion.mapper;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ProductRequestDTO;
import org.example.e_fashion.dto.response.ColorResponseDTO;
import org.example.e_fashion.dto.response.ProductResponseDTO;
import org.example.e_fashion.entity.ProductEntity;
import org.example.e_fashion.entity.ProductVariantEntity;
import org.example.e_fashion.entity.ProductVariantSizeEntity;
import org.example.e_fashion.service.ProductVariantImageService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ProductMapper {
    private final ProductVariantImageService productVariantImageService;

    public ProductEntity toEntity(ProductRequestDTO dto) {
        ProductEntity entity = new ProductEntity();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setIsActive(dto.getIsActive());
        entity.setThumbnailUrl(dto.getThumbnailUrl());
        return entity;
    }

    public ProductResponseDTO toResponse(ProductEntity entity) {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSlug(entity.getSlug());

        dto.setDescription(entity.getDescription());
        dto.setThumbnail(entity.getThumbnailUrl());

        dto.setIsActive(entity.getIsActive());

        dto.setDeletedAt(entity.getDeletedAt());

        if (entity.getCategory() != null) {
            dto.setCategoryId(entity.getCategory().getId());
            dto.setCategoryName(entity.getCategory().getName());
            dto.setCategorySlug(entity.getCategory().getSlug());
        }

        if (entity.getBrand() != null) {
            dto.setBrandId(entity.getBrand().getId());
            dto.setBrandName(entity.getBrand().getName());
            dto.setBrandSlug(entity.getBrand().getSlug());
        }

        // Variants
        if (entity.getVariants() != null && !entity.getVariants().isEmpty()) {

            // Colors
            dto.setColors(
                    entity.getVariants()
                            .stream()
                            .map(ProductVariantEntity::getColor)
                            .distinct()
                            .map(color -> {
                                ColorResponseDTO c = new ColorResponseDTO();
                                c.setId(color.getId());
                                c.setName(color.getName());
                                c.setSlug(color.getSlug());
                                return c;
                            })
                            .toList()
            );

            // ===== PRICE CALCULATION =====

            BigDecimal maxOriginal = BigDecimal.ZERO;
            BigDecimal minSale = null;

            for (ProductVariantEntity variant : entity.getVariants()) {

                if (variant.getSizes() == null) continue;

                for (ProductVariantSizeEntity size : variant.getSizes()) {

                    BigDecimal original = size.getOriginalPrice();
                    BigDecimal sale = size.getSalePrice();

                    if (original != null) {
                        if (original.compareTo(maxOriginal) > 0) {
                            maxOriginal = original;
                        }
                    }

                    BigDecimal effectivePrice =
                            (sale != null && sale.compareTo(BigDecimal.ZERO) > 0)
                                    ? sale
                                    : original;

                    if (effectivePrice != null) {
                        if (minSale == null ||
                                effectivePrice.compareTo(minSale) < 0) {
                            minSale = effectivePrice;
                        }
                    }
                }
            }

            dto.setOriginalPrice(maxOriginal);
            dto.setSalePrice(minSale != null ? minSale : maxOriginal);
        }

        return dto;
    }

    public void updateEntity(ProductEntity entity, ProductRequestDTO dto) {

        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }

        if (dto.getIsActive() != null) {
            entity.setIsActive(dto.getIsActive());
        }

        if (dto.getThumbnailUrl() != null) {
            entity.setThumbnailUrl(dto.getThumbnailUrl());
        }
    }

}