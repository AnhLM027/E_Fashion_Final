package org.example.e_fashion.dto.response;

import lombok.Data;

@Data
public class ColorResponseDTO {
    private String id;
    private String name;
    private String slug;
    private String code;
    private Boolean isActive;
}