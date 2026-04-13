package org.example.e_fashion.controller.client;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ProductRequestDTO;
import org.example.e_fashion.dto.response.ProductResponseDTO;
import org.example.e_fashion.entity.ProductEntity;
import org.example.e_fashion.mapper.ProductMapper;
import org.example.e_fashion.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getProducts(
            @RequestParam(required = false) List<String> categorySlugs,
            @RequestParam(required = false) List<String> brandSlugs,
            @RequestParam(required = false) List<String> colorSlugs,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {

        List<ProductEntity> products = productService.filterProducts(
                categorySlugs,
                brandSlugs,
                colorSlugs,
                minPrice,
                maxPrice
        );

        return ResponseEntity.ok(
                products.stream()
                        .map(productMapper::toResponse)
                        .toList()
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponseDTO>> searchProducts(
            @RequestParam String keyword
    ) {
        List<ProductEntity> products = productService.search(keyword);

        return ResponseEntity.ok(
                products.stream()
                        .map(productMapper::toResponse)
                        .toList()
        );
    }

    @GetMapping("/{id}")
    public ProductResponseDTO getById(@PathVariable String id) {
        return productMapper.toResponse(productService.getActiveProductById(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProductResponseDTO> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productMapper.toResponse(productService.getActiveProductBySlug(slug)));
    }
}