package org.example.e_fashion.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LowStockDTO {

    private String productName;
    private String colorName;
    private String sizeName;
    private Integer stock;
}