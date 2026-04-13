package org.example.e_fashion.service;

import org.example.e_fashion.dto.request.ProductAttributeRequestDTO;
import org.example.e_fashion.entity.ProductAttributeEntity;

import java.util.List;

public interface ProductAttributeService {
    ProductAttributeEntity create(ProductAttributeRequestDTO request);

    ProductAttributeEntity update(String id, ProductAttributeRequestDTO request);

    List<ProductAttributeEntity> getByProduct(String productId);

    void delete(String id);
}
