package org.example.e_fashion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ProductVariantRequestDTO;
import org.example.e_fashion.entity.ColorEntity;
import org.example.e_fashion.entity.ProductEntity;
import org.example.e_fashion.entity.ProductVariantEntity;
import org.example.e_fashion.mapper.ProductVariantMapper;
import org.example.e_fashion.repository.ColorRepository;
import org.example.e_fashion.repository.ProductRepository;
import org.example.e_fashion.repository.ProductVariantRepository;
import org.example.e_fashion.service.ProductVariantImageService;
import org.example.e_fashion.service.ProductVariantService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ColorRepository colorRepository;
    private final ProductVariantMapper productVariantMapper;
    private final ProductVariantImageService productVariantImageService;

    @Override
    public ProductVariantEntity create(ProductVariantRequestDTO request) {

        ProductEntity product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ColorEntity color = colorRepository.findById(request.getColorId())
                .orElseThrow(() -> new RuntimeException("Color not found"));

        ProductVariantEntity variant = productVariantMapper.toEntity(request, product, color);

        //        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
//
//            ProductVariantImageRequestDTO imageRequest = new ProductVariantImageRequestDTO();
//            imageRequest.setProductId(product.getId());
//            imageRequest.setImageUrl(request.getImageUrl());
//            imageRequest.setIsPrimary(true);
//
//            productVariantImageService.create(imageRequest);
//        }

        return variantRepository.save(variant);
    }

    @Override
    public ProductVariantEntity update(String id, ProductVariantRequestDTO request) {

        ProductVariantEntity variant = variantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        ColorEntity color = colorRepository.findById(request.getColorId())
                .orElseThrow(() -> new RuntimeException("Color not found"));

        if (request.getProductId() != null
                && !variant.getProduct().getId().equals(request.getProductId())) {

            ProductEntity product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            variant.setProduct(product);
        }

        productVariantMapper.updateEntity(variant, request, color);

        return variantRepository.save(variant);
    }

    @Override
    public List<ProductVariantEntity> getByProduct(String productId) {
        return variantRepository.findByProduct_Id(productId);
    }

    @Override
    public List<ProductVariantEntity> getByProductAndIsActive(String productId) {
        return variantRepository.findByProductIdAndIsActiveTrue(productId);
    }

    @Override
    public void softDeleteVariant(String id) {
        ProductVariantEntity variant = variantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        variant.setIsActive(false);
        variantRepository.save(variant);
    }

    @Override
    public void hardDeleteVariant(String id) {
        ProductVariantEntity variant = variantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Variant not found"));
        variantRepository.delete(variant);
    }
}