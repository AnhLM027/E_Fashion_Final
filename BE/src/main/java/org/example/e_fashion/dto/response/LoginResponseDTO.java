package org.example.e_fashion.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.e_fashion.entity.enums.RoleEnum;

@Getter @Setter
@Builder
public class LoginResponseDTO {
    private String accessToken;
    private String refreshToken;
    private RoleEnum role;
}
