package org.example.e_fashion.mapper;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.response.UserResponseDTO;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.entity.enums.RoleEnum;
import org.example.e_fashion.dto.request.RegisterRequestDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {
    private final ModelMapper modelMapper;

    public UserEntity toUserEntity(RegisterRequestDTO registerRequestDTO) {
        UserEntity userEntity = modelMapper.map(registerRequestDTO, UserEntity.class);
        userEntity.setRole(RoleEnum.CUSTOMER);
        userEntity.setIsActive(true);
        return userEntity;
    }

    public UserResponseDTO toResponse(UserEntity user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setGender(user.getGender());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setRole(user.getRole());
        return dto;
    }
}
