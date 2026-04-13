package org.example.e_fashion.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.example.e_fashion.entity.enums.GenderEnum;

@Getter
@Setter
public class UpdateProfileRequestDTO {

    private String fullName;
    private String phone;
    private GenderEnum gender;
    private String avatarUrl;
}
