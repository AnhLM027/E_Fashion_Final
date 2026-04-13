package org.example.e_fashion.controller.customer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.CartItemRequestDTO;
import org.example.e_fashion.dto.request.ChangeCartVariantRequestDTO;
import org.example.e_fashion.dto.response.CartResponseDTO;
import org.example.e_fashion.mapper.CartMapper;
import org.example.e_fashion.service.CartService;
import org.example.e_fashion.utils.ExtractUserUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/carts")
@RequiredArgsConstructor
public class CartCustomerController {

    private final CartService cartService;
    private final CartMapper cartMapper;
    private final ExtractUserUtils extractUserUtils;

    @GetMapping
    public CartResponseDTO getCart(HttpServletRequest request) {
        String userId = extractUserUtils.extract(request).getId();
        return cartMapper.toResponse(cartService.getCart(userId));
    }

    @PostMapping("/items")
    public CartResponseDTO addItem(
            HttpServletRequest request,
            @Valid @RequestBody CartItemRequestDTO requestDTO) {

        String userId = extractUserUtils.extract(request).getId();

        return cartMapper.toResponse(
                cartService.addItem(
                        userId,
                        requestDTO.getProductVariantSizeId(),
                        requestDTO.getQuantity()
                )
        );
    }

    @PutMapping("/items/{productVariantSizeId}")
    public CartResponseDTO updateItem(
            HttpServletRequest request,
            @PathVariable String productVariantSizeId,
            @RequestParam Integer quantity) {

        String userId = extractUserUtils.extract(request).getId();

        return cartMapper.toResponse(
                cartService.updateItem(userId, productVariantSizeId, quantity)
        );
    }

    @PutMapping("/items/change-variant")
    public CartResponseDTO changeVariant(
            HttpServletRequest request,
            @Valid @RequestBody ChangeCartVariantRequestDTO dto) {

        String userId = extractUserUtils.extract(request).getId();

        return cartMapper.toResponse(
                cartService.changeVariant(
                        userId,
                        dto.getOldVariantSizeId(),
                        dto.getNewVariantSizeId()
                )
        );
    }

    @DeleteMapping("/items/{productVariantSizeId}")
    public void removeItem(
            HttpServletRequest request,
            @PathVariable String productVariantSizeId) {

        String userId = extractUserUtils.extract(request).getId();
        cartService.removeItem(userId, productVariantSizeId);
    }
}
