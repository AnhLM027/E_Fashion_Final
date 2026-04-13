package org.example.e_fashion.controller.staff;

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

import java.util.List;

@RestController
@RequestMapping("/api/staff/products")
@RequiredArgsConstructor
public class ProductStaffController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    @PostMapping
    public ResponseEntity<ProductResponseDTO> create(
            @Valid @RequestBody ProductRequestDTO request
    ) {
        ProductEntity product = productService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productMapper.toResponse(product));
    }

    @GetMapping
    public List<ProductResponseDTO> getProductsForStaff(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) Boolean status
    ) {
        return productService.getProductsForStaff(categoryId, brandId, status)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductResponseDTO getById(@PathVariable String id) {
        ProductEntity product = productService.getActiveProductById(id);
        return productMapper.toResponse(product);
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

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> update(
            @PathVariable String id,
            @Valid @RequestBody ProductRequestDTO request
    ) {
        ProductEntity product = productService.update(id, request);
        return ResponseEntity.ok(productMapper.toResponse(product));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> setStatus(
            @PathVariable String id,
            @RequestParam boolean active
    ) {
        productService.setStatus(id, active);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable String id) {
        productService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
