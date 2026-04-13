package org.example.e_fashion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.AdminUserUpdateRequestDTO;
import org.example.e_fashion.dto.response.AdminUserResponseDTO;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.entity.enums.RoleEnum;
import org.example.e_fashion.mapper.AdminUserMapper;
import org.example.e_fashion.repository.UserRepository;
import org.example.e_fashion.service.AdminUserService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final AdminUserMapper mapper;

    @Override
    public Page<AdminUserResponseDTO> getUsers(
            String search,
            String role,
            Boolean active,
            int page,
            int size
    ) {

        Pageable pageable =
                PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<UserEntity> users;

        if (search != null && !search.isBlank()) {
            users = userRepository.findByEmailContainingIgnoreCase(search, pageable);
        } else if (role != null) {
            users = userRepository.findByRole(RoleEnum.valueOf(role), pageable);
        } else if (active != null) {
            users = userRepository.findByIsActive(active, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        return users.map(mapper::toResponse);
    }

    @Override
    public AdminUserResponseDTO getUserDetail(String id) {

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return mapper.toResponse(user);
    }

    @Override
    public AdminUserResponseDTO updateUser(
            String id,
            AdminUserUpdateRequestDTO request
    ) {

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getGender() != null)user.setGender(request.getGender());
        if (request.getRole() != null)user.setRole(request.getRole());
        if (request.getIsActive() != null)user.setIsActive(request.getIsActive());

        userRepository.save(user);

        return mapper.toResponse(user);
    }

    @Override
    public void deactivateUser(String id) {

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsActive(false);
        userRepository.save(user);
    }

    @Override
    public void activateUser(String id) {

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsActive(true);
        userRepository.save(user);
    }
}