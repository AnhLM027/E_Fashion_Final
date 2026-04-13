package org.example.e_fashion.service;

import org.example.e_fashion.dto.request.ProductVariantSizeRequestDTO;
import org.example.e_fashion.dto.response.ProductVariantSizeResponseDTO;

import java.util.List;

public interface ProductVariantSizeService {

    ProductVariantSizeResponseDTO create(ProductVariantSizeRequestDTO dto);

    ProductVariantSizeResponseDTO getById(String id);

    List<ProductVariantSizeResponseDTO> getByVariantId(String variantId);

    ProductVariantSizeResponseDTO update(String id, ProductVariantSizeRequestDTO dto);

    void delete(String id);
}