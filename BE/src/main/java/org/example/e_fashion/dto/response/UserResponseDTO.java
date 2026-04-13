package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;
import org.example.e_fashion.entity.enums.GenderEnum;
import org.example.e_fashion.entity.enums.RoleEnum;

@Getter
@Setter
public class UserResponseDTO {
    private String id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private GenderEnum gender;
    private String avatarUrl;
    private RoleEnum role;
}