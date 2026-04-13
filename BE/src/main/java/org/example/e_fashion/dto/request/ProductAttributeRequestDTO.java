package org.example.e_fashion.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductAttributeRequestDTO {
    private String productId;
    private String attributeName;
    private String attributeValue;
}