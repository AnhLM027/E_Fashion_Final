package org.example.e_fashion.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeaturedProductRequestDTO {

    @NotBlank
    private String type; // new | sale | bestsellers
}