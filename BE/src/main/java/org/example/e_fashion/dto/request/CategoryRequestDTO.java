package org.example.e_fashion.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequestDTO {
    @NotBlank(message = "Category name is required")
    private String name;

    private String parentId;

    private String imageUrl;

    private Boolean isActive = true;
}
