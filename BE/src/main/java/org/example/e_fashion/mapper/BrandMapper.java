package org.example.e_fashion.mapper;

import org.example.e_fashion.dto.request.BrandRequestDTO;
import org.example.e_fashion.dto.response.BrandResponseDTO;
import org.example.e_fashion.entity.BrandEntity;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {

    public BrandEntity toEntity(BrandRequestDTO dto) {
        BrandEntity entity = new BrandEntity();
        entity.setName(dto.getName());
        entity.setLogoUrl(dto.getLogoUrl());
        return entity;
    }

    public BrandResponseDTO toResponse(BrandEntity entity) {
        BrandResponseDTO dto = new BrandResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSlug(entity.getSlug());
        dto.setLogoUrl(entity.getLogoUrl());
        return dto;
    }
}