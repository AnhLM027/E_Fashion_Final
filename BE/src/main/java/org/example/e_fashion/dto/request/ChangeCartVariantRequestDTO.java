package org.example.e_fashion.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeCartVariantRequestDTO {

    @NotBlank
    private String oldVariantSizeId;

    @NotBlank
    private String newVariantSizeId;
}