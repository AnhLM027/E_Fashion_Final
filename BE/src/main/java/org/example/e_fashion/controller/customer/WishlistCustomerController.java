package org.example.e_fashion.controller.customer;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.WishlistRequestDTO;
import org.example.e_fashion.dto.response.WishlistResponseDTO;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.mapper.WishlistMapper;
import org.example.e_fashion.service.WishlistService;
import org.example.e_fashion.utils.ExtractUserUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/wishlist")
@RequiredArgsConstructor
public class WishlistCustomerController {

    private final ExtractUserUtils extractUserUtils;
    private final WishlistService wishlistService;
    private final WishlistMapper wishlistMapper;

    @PostMapping
    public void add(
            HttpServletRequest request,
            @RequestBody WishlistRequestDTO requestDTO
    ) {
        UserEntity user = extractUserUtils.extract(request);
        wishlistService.addToWishlist(user.getId(), requestDTO.getProductId());
    }

    @DeleteMapping("/{productId}")
    public void remove(
            HttpServletRequest request,
            @PathVariable String productId
    ) {
        UserEntity user = extractUserUtils.extract(request);
        wishlistService.removeFromWishlist(user.getId(), productId);
    }

    @GetMapping
    public List<WishlistResponseDTO> getMyWishlist(
            HttpServletRequest request
    ) {
        UserEntity user = extractUserUtils.extract(request);
        return wishlistService.getUserWishlist(user.getId())
                .stream()
                .map(wishlistMapper::toResponse)
                .toList()
                ;
    }
}
