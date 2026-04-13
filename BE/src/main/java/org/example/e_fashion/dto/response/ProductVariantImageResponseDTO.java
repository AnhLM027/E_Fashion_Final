package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVariantImageResponseDTO {
    private String id;
    private String productVariantId;

    private String imageUrl;
    private Boolean isPrimary;
    private Integer sortOrder;
}