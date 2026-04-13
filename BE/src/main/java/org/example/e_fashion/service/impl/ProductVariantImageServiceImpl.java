package org.example.e_fashion.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ProductVariantImageRequestDTO;
import org.example.e_fashion.entity.ProductVariantEntity;
import org.example.e_fashion.entity.ProductVariantImageEntity;
import org.example.e_fashion.mapper.ProductVariantImageMapper;
import org.example.e_fashion.repository.ProductVariantImageRepository;
import org.example.e_fashion.repository.ProductVariantRepository;
import org.example.e_fashion.service.ProductVariantImageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductVariantImageServiceImpl implements ProductVariantImageService {

    private final ProductVariantImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductVariantImageMapper imageMapper;

    @Override
    public ProductVariantImageEntity create(String variantId, ProductVariantImageRequestDTO request) {

        ProductVariantEntity productVariant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Integer maxSortOrder = productImageRepository
                .findMaxSortOrderByProductVariantId(productVariant.getId());

        ProductVariantImageEntity image = imageMapper.toEntity(productVariant.getId(), request);

        image.setSortOrder(maxSortOrder == null ? 1 : maxSortOrder + 1);

        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            productImageRepository.clearPrimaryByProductVariantId(productVariant.getId());
            image.setIsPrimary(true);
        } else image.setIsPrimary(maxSortOrder == null);

        return productImageRepository.save(image);
    }

    @Override
    public List<ProductVariantImageEntity> getByVariant(String productId) {
        return productImageRepository.findByProductVariantIdOrderBySortOrderAsc(productId);
    }

    @Override
    @Transactional
    public void setPrimary(String imageId) {
        ProductVariantImageEntity image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        String productVariantId = image.getProductVariant().getId();

        productImageRepository.clearPrimaryByProductVariantId(productVariantId);
        image.setIsPrimary(true);
        productImageRepository.save(image);
    }

    @Override
    @Transactional
    public void delete(String imageId) {
        ProductVariantImageEntity image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        String productVariantId = image.getProductVariant().getId();

        productImageRepository.delete(image);
        reindexSortOrder(productVariantId);
        ensurePrimaryImage(productVariantId);
    }

    private void reindexSortOrder(String productId) {
        List<ProductVariantImageEntity> images = productImageRepository
                .findByProductVariantIdOrderBySortOrderAsc(productId);

        int order = 1;
        for (ProductVariantImageEntity img : images) {
            img.setSortOrder(order++);
        }

        productImageRepository.saveAll(images);
    }

    private void ensurePrimaryImage(String productId) {
        List<ProductVariantImageEntity> images = productImageRepository
                .findByProductVariantIdOrderBySortOrderAsc(productId);

        if (images.isEmpty()) return;

        boolean hasPrimary = images.stream().anyMatch(ProductVariantImageEntity::getIsPrimary);

        if (!hasPrimary) {
            images.getFirst().setIsPrimary(true);
            productImageRepository.save(images.getFirst());
        }
    }
}

