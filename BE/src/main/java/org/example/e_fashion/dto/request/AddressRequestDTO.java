package org.example.e_fashion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddressRequestDTO {
    @NotBlank(message = "Receiver name is required")
    private String receiverName;

    @NotBlank(message = "Receiver phone is required")
    @Pattern(regexp = "^(0[0-9]{9})$", message = "Invalid phone number")
    private String receiverPhone;

    private String province;
    private String district;
    private String ward;

    @NotBlank(message = "Detail address is required")
    private String detailAddress;

    private Boolean isDefault = false;
}