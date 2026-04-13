package org.example.e_fashion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ProductRequestDTO;
import org.example.e_fashion.entity.BrandEntity;
import org.example.e_fashion.entity.CategoryEntity;
import org.example.e_fashion.entity.ProductEntity;
import org.example.e_fashion.mapper.ProductMapper;
import org.example.e_fashion.repository.*;
import org.example.e_fashion.service.ProductService;
import org.example.e_fashion.service.ProductVariantImageService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductVariantImageService productVariantImageService;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ColorRepository colorRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductEntity create(ProductRequestDTO request) {
        ProductEntity product = productMapper.toEntity(request);

        String slug = generateSlug(request.getName());
        if (productRepository.existsBySlug(slug)) {
            throw new RuntimeException("Product slug already exists");
        }
        product.setSlug(slug);

        if (request.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }

        if (request.getBrandId() != null) {
            BrandEntity brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new RuntimeException("Brand not found"));
            product.setBrand(brand);
        }

        product.setIsActive(Boolean.FALSE);

        //        if (request.getThumbnailUrl() != null && !request.getThumbnailUrl().isBlank()) {
//
//            ProductVariantImageRequestDTO imageRequest = new ProductVariantImageRequestDTO();
//            imageRequest.setProductVariantId(savedProduct.getId());
//            imageRequest.setImageUrl(request.getThumbnailUrl());
//            imageRequest.setIsPrimary(true);
//
//            productVariantImageService.create(imageRequest);
//        }

        return productRepository.save(product);
    }

    @Override
    public ProductEntity update(String id, ProductRequestDTO request) {

        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        productMapper.updateEntity(product, request);

        if (request.getName() != null && !request.getName().equals(product.getName())) {
            product.setName(request.getName());
            String slug = generateSlug(request.getName());
            if (productRepository.existsBySlug(slug)) {
                throw new RuntimeException("Product slug already exists");
            }
            product.setSlug(slug);
        }

        if (request.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }

        if (request.getBrandId() != null) {
            BrandEntity brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new RuntimeException("Brand not found"));
            product.setBrand(brand);
        }

        if (request.getIsActive() != null && request.getIsActive()) {
            long variantCount =
                    productVariantRepository.countByProduct_IdAndIsActiveTrue(product.getId());

            if (variantCount == 0) {
                throw new RuntimeException(
                        "Cannot activate product without at least one variant"
                );
            }
        }

        return productRepository.save(product);
    }

    @Override
    public List<ProductEntity> getActiveProducts() {
        return productRepository.findVisibleProducts();
    }

    @Override
    public List<ProductEntity> getProductsForAdmin() {
        return productRepository.findAll();
    }

    @Override
    public ProductEntity getActiveProductById(String id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    @Override
    public ProductEntity getActiveProductBySlug(String slug) {
        return productRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    private List<String> getAllCategoryIds(String categoryId) {
        List<String> ids = new java.util.ArrayList<>();
        ids.add(categoryId);

        List<CategoryEntity> children = categoryRepository.findByParent_Id(categoryId);

        for (CategoryEntity child : children) {
            ids.addAll(getAllCategoryIds(child.getId()));
        }

        return ids;
    }

    @Override
    public List<ProductEntity> getProductsByCategorySlug(String categorySlug) {

        CategoryEntity rootCategory =
                categoryRepository.findBySlug(categorySlug)
                        .orElseThrow(() -> new RuntimeException("Category not found"));

        List<String> categoryIds = getAllCategoryIds(rootCategory.getId());

        System.out.println("Slug: " + categorySlug);
        System.out.println("Root category: " + rootCategory.getName());

        return productRepository
                .findByCategory_IdInAndIsActiveTrueAndDeletedAtIsNull(categoryIds);
    }

    @Override
    public List<ProductEntity> getProductsByCategoryForAdmin(String categoryId) {

        List<String> categoryIds = getAllCategoryIds(categoryId);

        return productRepository
                .findByCategory_IdIn(categoryIds);
    }

    @Override
    public List<ProductEntity> filterProducts(
            List<String> categorySlugs,
            List<String> brandSlugs,
            List<String> colorSlugs,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {

        if (minPrice == null) minPrice = BigDecimal.ZERO;
        if (maxPrice == null) maxPrice = new BigDecimal("999999999");

        List<String> categoryIds = null;
        List<String> brandIds = null;
        List<String> colorIds = null;

        // CATEGORY
        if (categorySlugs != null && !categorySlugs.isEmpty()) {

            categoryIds = new java.util.ArrayList<>();

            for (String slug : categorySlugs) {
                CategoryEntity category = categoryRepository.findBySlug(slug)
                        .orElseThrow(() -> new RuntimeException("Category not found: " + slug));

                categoryIds.addAll(getAllCategoryIds(category.getId()));
            }
        }

        // BRAND
        if (brandSlugs != null && !brandSlugs.isEmpty()) {

            brandIds = brandSlugs.stream()
                    .map(slug -> brandRepository.findBySlug(slug)
                            .orElseThrow(() -> new RuntimeException("Brand not found: " + slug)))
                    .map(BrandEntity::getId)
                    .toList();
        }

        // COLOR
        if (colorSlugs != null && !colorSlugs.isEmpty()) {

            colorIds = colorSlugs.stream()
                    .map(slug -> colorRepository.findBySlug(slug)
                            .orElseThrow(() -> new RuntimeException("Color not found: " + slug)))
                    .map(color -> color.getId())
                    .toList();
        }

        return productRepository.filterProducts(
                categoryIds,
                brandIds,
                colorIds,
                minPrice,
                maxPrice
        );
    }

    @Override
    public List<ProductEntity> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, 6); // trang 0, tối đa 8 kết quả

        return productRepository.search(keyword.trim(), pageable);
    }

    @Override
    public void setStatus(String id, boolean isActive) {
        ProductEntity product = productRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (isActive) {
            long variantCount =
                    productVariantRepository.countByProduct_IdAndIsActiveTrue(id);

            if (variantCount == 0) {
                throw new RuntimeException(
                        "Cannot activate product without at least one variant"
                );
            }
        }

        product.setIsActive(isActive);
        productRepository.save(product);
    }

    @Override
    public void hardDelete(String id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        productRepository.delete(product);
    }

    @Override
    public void softDelete(String id) {
        ProductEntity product = productRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setDeletedAt(java.time.LocalDateTime.now());
        product.setIsActive(false);

        productRepository.save(product);
    }

    @Override
    public void restore(String id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setDeletedAt(null);

        productRepository.save(product);
    }

    @Override
    public List<ProductEntity> getProductsForStaff(String categoryId, String brandId, Boolean isActive) {
        List<String> categoryIds = null;
        if (categoryId != null && !categoryId.isBlank()) {
            categoryIds = getAllCategoryIds(categoryId);
        }

        return productRepository.getProductsForStaff(categoryIds, brandId, isActive);
    }

    private String generateSlug(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}