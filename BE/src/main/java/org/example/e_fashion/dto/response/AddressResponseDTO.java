package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddressResponseDTO {
    private String id;

    private String receiverName;
    private String receiverPhone;

    private String province;
    private String district;
    private String ward;
    private String detailAddress;

    private Boolean isDefault;
}