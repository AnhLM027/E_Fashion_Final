package org.example.e_fashion.mapper;

import org.example.e_fashion.dto.response.ColorResponseDTO;
import org.example.e_fashion.entity.ColorEntity;
import org.springframework.stereotype.Component;

@Component
public class ColorMapper {

    public ColorResponseDTO toResponse(ColorEntity entity) {
        ColorResponseDTO dto = new ColorResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSlug(entity.getSlug());
        dto.setCode(entity.getCode());
        dto.setIsActive(entity.getIsActive());
        return dto;
    }
}