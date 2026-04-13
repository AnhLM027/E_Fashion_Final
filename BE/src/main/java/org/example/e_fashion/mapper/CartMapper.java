package org.example.e_fashion.mapper;

import org.example.e_fashion.dto.request.CartVariantDTO;
import org.example.e_fashion.dto.response.CartItemResponseDTO;
import org.example.e_fashion.dto.response.CartResponseDTO;
import org.example.e_fashion.entity.CartEntity;
import org.example.e_fashion.entity.CartItemEntity;
import org.example.e_fashion.entity.ProductVariantSizeEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartMapper {

    public CartResponseDTO toResponse(CartEntity cart) {

        CartResponseDTO dto = new CartResponseDTO();
        dto.setCartId(cart.getId());
        dto.setUserId(cart.getUser().getId());

        List<CartItemResponseDTO> items = cart.getItems()
                .stream()
                .map(this::toItemResponse)
                .toList();

        dto.setItems(items);

        BigDecimal total = items.stream()
                .map(i -> i.getPrice()
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setTotalPrice(total);

        return dto;
    }

    private CartItemResponseDTO toItemResponse(CartItemEntity item) {
        var variantSize = item.getProductVariantSize();
        var variant = variantSize.getProductVariant();
        var product = variant.getProduct();
        var color = variant.getColor();

        CartItemResponseDTO dto = new CartItemResponseDTO();

        dto.setSlug(product.getSlug());

        dto.setProductVariantSizeId(variantSize.getId());
        dto.setQuantity(item.getQuantity());

        dto.setProductId(product.getId());
        dto.setProductName(product.getName());

        dto.setColorName(color.getName());
        dto.setSizeName(variantSize.getSizeName());

        BigDecimal price = (variantSize.getSalePrice() != null
                && variantSize.getSalePrice().compareTo(BigDecimal.ZERO) > 0)
                ? variantSize.getSalePrice()
                : variantSize.getOriginalPrice();

        dto.setPrice(price);

        dto.setProductImage(product.getThumbnailUrl());

        int available = variantSize.getStock() - variantSize.getReservedStock();
        dto.setAvailableStock(available);
        dto.setOutOfStock(available <= 0);

        List<String> colors = product.getVariants()
                .stream()
                .map(v -> v.getColor().getName())
                .distinct()
                .toList();

        dto.setColors(colors);

        List<String> sizes = product.getVariants()
                .stream()
                .flatMap(v -> v.getSizes().stream())
                .map(ProductVariantSizeEntity::getSizeName)
                .distinct()
                .toList();

        dto.setSizes(sizes);

        List<CartVariantDTO> variantSizes = product.getVariants()
                .stream()
                .flatMap(v -> v.getSizes().stream())
                .map(vs -> {

                    CartVariantDTO variantDTO = new CartVariantDTO();

                    variantDTO.setId(vs.getId());
                    variantDTO.setColorName(
                            vs.getProductVariant().getColor().getName()
                    );

                    variantDTO.setSizeName(vs.getSizeName());

                    int stock = vs.getStock() - vs.getReservedStock();
                    variantDTO.setStock(stock);

                    return variantDTO;
                })
                .collect(Collectors.toList());

        dto.setVariantSizes(variantSizes);

        return dto;
    }
}
