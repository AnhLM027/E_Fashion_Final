package org.example.e_fashion.controller.client;

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
@RequestMapping("/api/products/{productId}/attributes")
public class ProductAttributeController {

    private final ProductAttributeService attributeService;
    private final ProductAttributeMapper attributeMapper;

    @GetMapping
    public List<ProductAttributeResponseDTO> getByProduct(
            @PathVariable String productId
    ) {
        return attributeService.getByProduct(productId)
                .stream()
                .map(attributeMapper::toResponse)
                .toList();
    }
}
