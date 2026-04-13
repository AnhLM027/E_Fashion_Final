package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;
import org.example.e_fashion.entity.enums.GenderEnum;
import org.example.e_fashion.entity.enums.RoleEnum;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminUserResponseDTO {

    private String id;
    private String email;
    private String fullName;
    private String phone;
    private GenderEnum gender;
    private String avatarUrl;
    private RoleEnum role;
    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}