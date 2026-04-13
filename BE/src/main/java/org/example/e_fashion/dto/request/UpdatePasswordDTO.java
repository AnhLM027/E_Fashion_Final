package org.example.e_fashion.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdatePasswordDTO {
    String email;
    String oldPassword;
    String newPassword;
}
