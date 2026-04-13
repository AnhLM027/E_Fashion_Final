package org.example.e_fashion.mapper;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.CategoryRequestDTO;
import org.example.e_fashion.dto.response.CategoryResponseDTO;
import org.example.e_fashion.dto.response.CategoryTreeDTO;
import org.example.e_fashion.entity.CategoryEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CategoryMapper {

    public CategoryEntity toEntity(CategoryRequestDTO dto) {
        CategoryEntity entity = new CategoryEntity();

        entity.setName(dto.getName());
        entity.setImageUrl(dto.getImageUrl());
        entity.setIsActive(dto.getIsActive());

        return entity;
    }

    public CategoryResponseDTO toResponse(CategoryEntity entity) {
        if (entity == null) return null;

        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSlug(entity.getSlug());
        dto.setImageUrl(entity.getImageUrl());
        dto.setIsActive(entity.getIsActive());

        if (entity.getParent() != null) {
            dto.setParentId(entity.getParent().getId());
        }

        return dto;
    }

    public CategoryTreeDTO toTree(CategoryEntity entity) {

        CategoryTreeDTO dto = new CategoryTreeDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSlug(entity.getSlug());
        dto.setImageUrl(entity.getImageUrl());

        if (entity.getParent() != null) {
            dto.setParentId(entity.getParent().getId());
        }

        dto.setIsActive(entity.getIsActive());

        if (entity.getChildren() != null && !entity.getChildren().isEmpty()) {
            dto.setChildren(
                    entity.getChildren()
                            .stream()
                            .map(this::toTree)
                            .toList()
            );
        }

        return dto;
    }

    public void updateEntity(CategoryEntity entity, CategoryRequestDTO dto) {
        entity.setName(dto.getName());
        entity.setImageUrl(dto.getImageUrl());
        entity.setIsActive(dto.getIsActive());
    }
}
