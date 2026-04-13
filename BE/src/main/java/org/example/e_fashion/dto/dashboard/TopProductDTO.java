package org.example.e_fashion.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TopProductDTO {

    private String productId;
    private String productName;
    private Long totalSold;
}