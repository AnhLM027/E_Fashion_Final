package org.example.e_fashion.controller.staff;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ProductAttributeRequestDTO;
import org.example.e_fashion.dto.response.ProductAttributeResponseDTO;
import org.example.e_fashion.entity.ProductAttributeEntity;
import org.example.e_fashion.mapper.ProductAttributeMapper;
import org.example.e_fashion.service.ProductAttributeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/staff/products/{productId}/attributes")
public class ProductAttributeStaffController {

    private final ProductAttributeService attributeService;
    private final ProductAttributeMapper attributeMapper;

    @PostMapping
    public ResponseEntity<ProductAttributeResponseDTO> create(
            @PathVariable String productId,
            @RequestBody ProductAttributeRequestDTO request
    ) {
        request.setProductId(productId);
        ProductAttributeEntity attr = attributeService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attributeMapper.toResponse(attr));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductAttributeResponseDTO> update(
            @PathVariable String productId,
            @PathVariable String id,
            @RequestBody ProductAttributeRequestDTO request
    ) {
        request.setProductId(productId);

        ProductAttributeEntity attr = attributeService.update(id, request);

        return ResponseEntity.ok(attributeMapper.toResponse(attr));
    }

    @GetMapping
    public List<ProductAttributeResponseDTO> getByProduct(
            @PathVariable String productId
    ) {
        return attributeService.getByProduct(productId)
                .stream()
                .map(attributeMapper::toResponse)
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        attributeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
