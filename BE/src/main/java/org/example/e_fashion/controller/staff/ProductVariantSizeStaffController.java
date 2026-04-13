package org.example.e_fashion.controller.staff;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ProductVariantSizeRequestDTO;
import org.example.e_fashion.dto.response.ProductVariantSizeResponseDTO;
import org.example.e_fashion.service.ProductVariantSizeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/product-variant-sizes")
@RequiredArgsConstructor
public class ProductVariantSizeStaffController {

    private final ProductVariantSizeService service;

    @PostMapping
    public ResponseEntity<ProductVariantSizeResponseDTO> create(
            @RequestBody ProductVariantSizeRequestDTO request
    ) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductVariantSizeResponseDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/variant/{variantId}")
    public ResponseEntity<List<ProductVariantSizeResponseDTO>> getByVariant(
            @PathVariable String variantId
    ) {
        return ResponseEntity.ok(service.getByVariantId(variantId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductVariantSizeResponseDTO> update(
            @PathVariable String id,
            @RequestBody ProductVariantSizeRequestDTO request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
