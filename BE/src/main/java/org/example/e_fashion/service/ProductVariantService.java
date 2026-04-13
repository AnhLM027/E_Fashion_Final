package org.example.e_fashion.service;

import org.example.e_fashion.dto.request.ProductVariantRequestDTO;
import org.example.e_fashion.entity.ProductVariantEntity;

import java.util.List;

public interface ProductVariantService {

    ProductVariantEntity create(ProductVariantRequestDTO request);

    ProductVariantEntity update(String id, ProductVariantRequestDTO request);

    List<ProductVariantEntity> getByProduct(String productId);

    List<ProductVariantEntity> getByProductAndIsActive(String productId);

    void softDeleteVariant(String id);

    void hardDeleteVariant(String id);
}