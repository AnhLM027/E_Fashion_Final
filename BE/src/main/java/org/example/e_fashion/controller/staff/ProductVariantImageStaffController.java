package org.example.e_fashion.controller.staff;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ProductVariantImageRequestDTO;
import org.example.e_fashion.dto.response.ProductVariantImageResponseDTO;
import org.example.e_fashion.entity.ProductVariantImageEntity;
import org.example.e_fashion.mapper.ProductVariantImageMapper;
import org.example.e_fashion.service.ProductVariantImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/staff/product-variants/{variantId}/images")
public class ProductVariantImageStaffController {

    private final ProductVariantImageService variantImageService;
    private final ProductVariantImageMapper imageMapper;

    @PostMapping
    public ResponseEntity<ProductVariantImageResponseDTO> create(
            @PathVariable String variantId,
            @RequestBody ProductVariantImageRequestDTO request
    ) {
        ProductVariantImageEntity image = variantImageService.create(variantId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(imageMapper.toResponse(image));
    }

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

    @PatchMapping("/{id}/primary")
    public ResponseEntity<Void> setPrimary(
            @PathVariable String variantId,
            @PathVariable String id
    ) {
        variantImageService.setPrimary(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String variantId,
            @PathVariable String id
    ) {
        variantImageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
