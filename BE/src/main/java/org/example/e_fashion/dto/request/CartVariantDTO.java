package org.example.e_fashion.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartVariantDTO {

    private String id;

    private String colorName;
    private String sizeName;

    private Integer stock;
}
