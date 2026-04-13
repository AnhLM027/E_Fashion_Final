package org.example.e_fashion.service;

import org.example.e_fashion.dto.request.ProductVariantImageRequestDTO;
import org.example.e_fashion.entity.ProductVariantImageEntity;

import java.util.List;

public interface ProductVariantImageService {
    ProductVariantImageEntity create(String variantId, ProductVariantImageRequestDTO request);
    List<ProductVariantImageEntity> getByVariant(String variantId);

    void setPrimary(String imageId);

    void delete(String id);
}
