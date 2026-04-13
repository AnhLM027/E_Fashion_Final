package org.example.e_fashion.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CartItemRequestDTO {

    @NotBlank
    private String productVariantSizeId;

    @Min(1)
    private Integer quantity = 1;
}
