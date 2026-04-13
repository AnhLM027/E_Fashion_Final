package org.example.e_fashion.mapper;

import org.example.e_fashion.dto.request.ProductAttributeRequestDTO;
import org.example.e_fashion.dto.response.ProductAttributeResponseDTO;
import org.example.e_fashion.entity.ProductAttributeEntity;
import org.example.e_fashion.entity.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductAttributeMapper {

    public ProductAttributeEntity toEntity(
            ProductAttributeRequestDTO dto,
            ProductEntity product
    ) {
        ProductAttributeEntity entity = new ProductAttributeEntity();
        entity.setProduct(product);
        entity.setAttributeName(dto.getAttributeName());
        entity.setAttributeValue(dto.getAttributeValue());
        return entity;
    }

    public ProductAttributeResponseDTO toResponse(ProductAttributeEntity entity) {
        ProductAttributeResponseDTO dto = new ProductAttributeResponseDTO();
        dto.setId(entity.getId());
        dto.setAttributeName(entity.getAttributeName());
        dto.setAttributeValue(entity.getAttributeValue());
        dto.setProductId(entity.getProduct().getId());
        return dto;
    }

    public void updateEntity(ProductAttributeEntity entity, ProductAttributeRequestDTO dto) {

        if (dto.getAttributeName() != null) entity.setAttributeName(dto.getAttributeName());
        if (dto.getAttributeValue() != null) entity.setAttributeValue(dto.getAttributeValue());
    }
}