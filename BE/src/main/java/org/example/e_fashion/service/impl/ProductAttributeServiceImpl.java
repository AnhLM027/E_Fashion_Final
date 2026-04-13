package org.example.e_fashion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ProductAttributeRequestDTO;
import org.example.e_fashion.entity.ProductAttributeEntity;
import org.example.e_fashion.entity.ProductEntity;
import org.example.e_fashion.mapper.ProductAttributeMapper;
import org.example.e_fashion.repository.ProductAttributeRepository;
import org.example.e_fashion.repository.ProductRepository;
import org.example.e_fashion.service.ProductAttributeService;
import org.example.e_fashion.exceptions.GlobalExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductAttributeServiceImpl implements ProductAttributeService {

    private final ProductAttributeRepository attributeRepository;
    private final ProductRepository productRepository;
    private final ProductAttributeMapper attributeMapper;

    @Override
    public ProductAttributeEntity create(ProductAttributeRequestDTO request) {

        ProductEntity product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                )
        );

        if (attributeRepository.existsByProduct_IdAndAttributeName(
                product.getId(), request.getAttributeName())) {
            throw new RuntimeException("Attribute already exists for product");
        }

        ProductAttributeEntity attribute =
                attributeMapper.toEntity(request, product);

        return attributeRepository.save(attribute);
    }

    @Override
    public ProductAttributeEntity update(String id, ProductAttributeRequestDTO request) {

        ProductAttributeEntity attribute = attributeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute not found"));

        // đảm bảo attribute thuộc đúng product
        if (!attribute.getProduct().getId().equals(request.getProductId())) {
            throw new RuntimeException("Attribute does not belong to this product");
        }

        attributeMapper.updateEntity(attribute, request);

        return attributeRepository.save(attribute);
    }

    @Override
    public List<ProductAttributeEntity> getByProduct(String productId) {

        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found"
            );
        }

        return attributeRepository.findByProduct_Id(productId);
    }

    @Override
    public void delete(String id) {

        ProductAttributeEntity attribute = attributeRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product attribute not found"
                        )
                );

        attributeRepository.delete(attribute);
    }
}
