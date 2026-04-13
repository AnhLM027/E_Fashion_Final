package org.example.e_fashion.service;

import org.example.e_fashion.dto.request.ProductRequestDTO;
import org.example.e_fashion.entity.ProductEntity;
import org.springframework.beans.PropertyValues;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    ProductEntity create(ProductRequestDTO request);

    List<ProductEntity> getActiveProducts();

    List<ProductEntity> getProductsForAdmin();

    ProductEntity getActiveProductById(String id);

    ProductEntity getActiveProductBySlug(String slug);

    List<ProductEntity> getProductsByCategorySlug(String categorySlug);

    List<ProductEntity> filterProducts(
            List<String> categorySlugs,
            List<String> brandSlugs,
            List<String> colorSlugs,
            BigDecimal minPrice,
            BigDecimal maxPrice
    );

    List<ProductEntity> search(String keyword);

    ProductEntity update(String id, ProductRequestDTO request);

    void setStatus(String id, boolean isActive);

    void hardDelete(String id);

    void softDelete(String id);

    void restore(String id);

    List<ProductEntity> getProductsByCategoryForAdmin(String categoryId);

    List<ProductEntity> getProductsForStaff(String categoryId, String brandId, Boolean isActive);
}
