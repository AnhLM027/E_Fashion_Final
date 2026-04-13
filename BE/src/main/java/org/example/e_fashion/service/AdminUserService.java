package org.example.e_fashion.service;

import org.example.e_fashion.dto.request.AdminUserUpdateRequestDTO;
import org.example.e_fashion.dto.response.AdminUserResponseDTO;
import org.springframework.data.domain.Page;

public interface AdminUserService {

    Page<AdminUserResponseDTO> getUsers(
            String search,
            String role,
            Boolean active,
            int page,
            int size
    );

    AdminUserResponseDTO getUserDetail(String id);

    AdminUserResponseDTO updateUser(
            String id,
            AdminUserUpdateRequestDTO request
    );

    void deactivateUser(String id);

    void activateUser(String id);
}