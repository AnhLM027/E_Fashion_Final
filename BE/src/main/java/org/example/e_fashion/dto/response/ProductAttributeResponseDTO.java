package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductAttributeResponseDTO {
    private String id;
    private String attributeName;
    private String attributeValue;

    private String productId;
}
