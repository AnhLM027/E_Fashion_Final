package org.example.e_fashion.service.impl;

import org.example.e_fashion.dto.request.UpdatePasswordDTO;
import org.example.e_fashion.dto.request.UpdateProfileRequestDTO;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.repository.UserRepository;
import org.example.e_fashion.service.UserService;
import org.example.e_fashion.utils.ExtractUserUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final ExtractUserUtils extractUserUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserEntity getProfile(HttpServletRequest request) {
        return extractUserUtils.extract(request);
    }

    @Override
    public UserEntity updateProfile(HttpServletRequest request, UpdateProfileRequestDTO profileRequestDTO) {

        UserEntity user = extractUserUtils.extract(request);

        if (profileRequestDTO.getFullName() != null) {
            user.setFullName(profileRequestDTO.getFullName());
        }

        if (profileRequestDTO.getPhone() != null) {
            user.setPhone(profileRequestDTO.getPhone());
        }

        if (profileRequestDTO.getGender() != null) {
            user.setGender(profileRequestDTO.getGender());
        }

        if (profileRequestDTO.getAvatarUrl() != null) {
            user.setAvatarUrl(profileRequestDTO.getAvatarUrl());
        }

        return userRepository.save(user);
    }

    @Override
    public void updatePassword(HttpServletRequest request, UpdatePasswordDTO updatePasswordDTO) {

        UserEntity user = extractUserUtils.extract(request);

        if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Old password is incorrect"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(updatePasswordDTO.getNewPassword())
        );

        userRepository.save(user);
    }
}
