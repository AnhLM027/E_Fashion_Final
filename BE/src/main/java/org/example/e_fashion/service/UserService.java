package org.example.e_fashion.service;

import jakarta.servlet.http.HttpServletRequest;
import org.example.e_fashion.dto.request.LoginRequestDTO;
import org.example.e_fashion.dto.request.RegisterRequestDTO;
import org.example.e_fashion.dto.request.UpdatePasswordDTO;
import org.example.e_fashion.dto.request.UpdateProfileRequestDTO;
import org.example.e_fashion.dto.response.LoginResponseDTO;
import org.example.e_fashion.entity.UserEntity;

public interface UserService {

    UserEntity getProfile(HttpServletRequest request);

    UserEntity updateProfile(HttpServletRequest request, UpdateProfileRequestDTO profileRequestDTO);

    void updatePassword(HttpServletRequest request, UpdatePasswordDTO updatePasswordDTO);
}
