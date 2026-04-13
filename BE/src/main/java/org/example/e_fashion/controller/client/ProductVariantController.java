package org.example.e_fashion.controller.client;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ProductVariantRequestDTO;
import org.example.e_fashion.dto.response.ProductVariantResponseDTO;
import org.example.e_fashion.mapper.ProductVariantMapper;
import org.example.e_fashion.service.ProductVariantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-variants")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantService variantService;
    private final ProductVariantMapper variantMapper;

    @GetMapping("/product/{productId}")
    public List<ProductVariantResponseDTO> getByProduct(@PathVariable String productId) {
        return variantService.getByProductAndIsActive(productId)
                .stream()
                .map(variantMapper::toResponse)
                .toList();
    }
}