package org.example.e_fashion.controller.client;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.response.ProductVariantImageResponseDTO;
import org.example.e_fashion.mapper.ProductVariantImageMapper;
import org.example.e_fashion.service.ProductVariantImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product-variants/{variantId}/images")
public class ProductImageController {

    private final ProductVariantImageService variantImageService;
    private final ProductVariantImageMapper imageMapper;

    @GetMapping
    public ResponseEntity<List<ProductVariantImageResponseDTO>> getByVariant(
            @PathVariable String variantId
    ) {
        return ResponseEntity.ok(
                variantImageService.getByVariant(variantId)
                        .stream()
                        .map(imageMapper::toResponse)
                        .toList());
    }
}