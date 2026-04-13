package org.example.e_fashion.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LoginRequestDTO {
    @NotBlank(message = "Email is not blank")
    private String email;

    @NotBlank(message = "Password is not blank")
    private String password;
}
