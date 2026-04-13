package org.example.e_fashion.controller.staff;

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
@RequestMapping("/api/staff/product-variants")
@RequiredArgsConstructor
public class ProductVariantStaffController {

    private final ProductVariantService variantService;
    private final ProductVariantMapper variantMapper;

    @PostMapping
    public ResponseEntity<ProductVariantResponseDTO> create(
            @RequestBody ProductVariantRequestDTO request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(variantMapper.toResponse(
                        variantService.create(request)));
    }

    @GetMapping("/product/{productId}")
    public List<ProductVariantResponseDTO> getByProduct(@PathVariable String productId) {
        return variantService.getByProduct(productId)
                .stream()
                .map(variantMapper::toResponse)
                .toList();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductVariantResponseDTO> update(
            @PathVariable String id,
            @RequestBody ProductVariantRequestDTO request
    ) {
        return ResponseEntity.ok(
                variantMapper.toResponse(
                        variantService.update(id, request)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable String id) {
        variantService.softDeleteVariant(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDelete(@PathVariable String id) {
        variantService.hardDeleteVariant(id);
        return ResponseEntity.noContent().build();
    }
}
