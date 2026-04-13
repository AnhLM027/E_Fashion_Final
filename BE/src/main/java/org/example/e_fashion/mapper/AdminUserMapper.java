package org.example.e_fashion.mapper;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.response.AdminUserResponseDTO;
import org.example.e_fashion.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminUserMapper {

    public AdminUserResponseDTO toResponse(UserEntity user) {

        AdminUserResponseDTO dto = new AdminUserResponseDTO();

        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setGender(user.getGender());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setRole(user.getRole());
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        return dto;
    }
}