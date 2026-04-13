package org.example.e_fashion.mapper;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ProductVariantSizeRequestDTO;
import org.example.e_fashion.dto.response.ProductVariantSizeResponseDTO;
import org.example.e_fashion.entity.ProductVariantEntity;
import org.example.e_fashion.entity.ProductVariantSizeEntity;
import org.example.e_fashion.repository.ProductVariantRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductVariantSizeMapper {

    private final ProductVariantRepository productVariantRepository;

    public ProductVariantSizeEntity toEntity(ProductVariantSizeRequestDTO dto) {

        ProductVariantEntity variant = productVariantRepository
                .findById(dto.getProductVariantId())
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        ProductVariantSizeEntity entity = new ProductVariantSizeEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setProductVariant(variant);
        entity.setSku(dto.getSku());
        entity.setSizeName(dto.getSizeName());
        entity.setOriginalPrice(dto.getOriginalPrice());
        entity.setSalePrice(dto.getSalePrice());
        entity.setStock(dto.getStock());
        entity.setReservedStock(0);

        return entity;
    }

    public ProductVariantSizeResponseDTO toResponse(ProductVariantSizeEntity entity) {

        ProductVariantSizeResponseDTO dto = new ProductVariantSizeResponseDTO();

        dto.setId(entity.getId());
        dto.setSku(entity.getSku());
        dto.setProductVariantId(entity.getProductVariant().getId());
        dto.setSizeName(entity.getSizeName());
        dto.setOriginalPrice(entity.getOriginalPrice());
        dto.setSalePrice(entity.getSalePrice());
        dto.setStock(entity.getStock());
        dto.setReservedStock(entity.getReservedStock());

        int available =
                entity.getStock() - entity.getReservedStock();

        dto.setAvailableStock(Math.max(available, 0));

        return dto;
    }

    public void update(ProductVariantSizeEntity entity, ProductVariantSizeRequestDTO dto) {

        if (dto.getSku() != null) entity.setSku(dto.getSku());
        if (dto.getSizeName() != null) entity.setSizeName(dto.getSizeName());
        if (dto.getOriginalPrice() != null) entity.setOriginalPrice(dto.getOriginalPrice());
        if (dto.getSalePrice() != null) entity.setSalePrice(dto.getSalePrice());
        if (dto.getStock() != null) {

            int newStock = dto.getStock();
            int reserved = entity.getReservedStock() != null
                    ? entity.getReservedStock()
                    : 0;

            if (newStock < reserved) {
                throw new RuntimeException(
                        "Stock cannot be less than reserved stock (" + reserved + ")"
                );
            }

            entity.setStock(newStock);
        }
    }
}