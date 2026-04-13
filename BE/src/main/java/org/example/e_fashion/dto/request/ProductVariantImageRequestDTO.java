package org.example.e_fashion.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVariantImageRequestDTO {
    private String imageUrl;
    private Boolean isPrimary = false;
    private Integer sortOrder = 0;
}
