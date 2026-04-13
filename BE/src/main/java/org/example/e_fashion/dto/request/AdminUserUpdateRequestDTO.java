package org.example.e_fashion.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.example.e_fashion.entity.enums.GenderEnum;
import org.example.e_fashion.entity.enums.RoleEnum;

@Getter
@Setter
public class AdminUserUpdateRequestDTO {

    private String fullName;
    private String phone;
    private GenderEnum gender;
    private RoleEnum role;
    private Boolean isActive;
}