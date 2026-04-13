package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
public class CartResponseDTO {

    private String cartId;
    private String userId;
    private List<CartItemResponseDTO> items;
    private BigDecimal totalPrice;
}
