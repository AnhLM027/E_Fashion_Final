package org.example.e_fashion.controller.customer;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.UpdatePasswordDTO;
import org.example.e_fashion.dto.request.UpdateProfileRequestDTO;
import org.example.e_fashion.dto.response.UserResponseDTO;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.mapper.UserMapper;
import org.example.e_fashion.service.UserService;
import org.example.e_fashion.utils.ExtractUserUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/profile")
@RequiredArgsConstructor
public class ProfileCustomerController {
    private final ExtractUserUtils extractUserUtils;
    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    public ResponseEntity<UserResponseDTO> getProfile(
            HttpServletRequest request
    ) {
        try {
            UserEntity user = extractUserUtils.extract(request);
            return ResponseEntity.ok(userMapper.toResponse(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping
    public ResponseEntity<UserResponseDTO> updateProfile(
            HttpServletRequest request,
            @RequestBody UpdateProfileRequestDTO updateRequest
    ) {
        UserEntity updatedUser = userService.updateProfile(
                request,
                updateRequest
        );

        return ResponseEntity.ok(userMapper.toResponse(updatedUser));
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(
            HttpServletRequest request,
            @RequestBody UpdatePasswordDTO passwordDTO
    ) {
        userService.updatePassword(request, passwordDTO);

        return ResponseEntity.ok().body("Password updated successfully");
    }
}
