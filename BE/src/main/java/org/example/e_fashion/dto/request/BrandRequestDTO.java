package org.example.e_fashion.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandRequestDTO {

    @NotBlank(message = "Brand name is required")
    private String name;

    private String logoUrl;
}