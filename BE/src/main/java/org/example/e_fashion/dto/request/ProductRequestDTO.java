package org.example.e_fashion.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequestDTO {

    @NotBlank
    private String name;

    private String description;

    private String categoryId;

    private String brandId;

    private String thumbnailUrl;

    private Boolean isActive = false;
}