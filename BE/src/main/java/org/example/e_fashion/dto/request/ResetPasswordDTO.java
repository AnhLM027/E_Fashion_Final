package org.example.e_fashion.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ResetPasswordDTO {
    private String token;
    private String newPassword;
}
