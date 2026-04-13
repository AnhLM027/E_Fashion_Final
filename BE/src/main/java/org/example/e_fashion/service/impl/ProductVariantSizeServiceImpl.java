package org.example.e_fashion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ProductVariantSizeRequestDTO;
import org.example.e_fashion.dto.response.ProductVariantSizeResponseDTO;
import org.example.e_fashion.entity.ProductVariantSizeEntity;
import org.example.e_fashion.mapper.ProductVariantSizeMapper;
import org.example.e_fashion.repository.ProductVariantSizeRepository;
import org.example.e_fashion.service.ProductVariantSizeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantSizeServiceImpl
        implements ProductVariantSizeService {

    private final ProductVariantSizeRepository repository;
    private final ProductVariantSizeMapper mapper;

    @Override
    public ProductVariantSizeResponseDTO create(ProductVariantSizeRequestDTO dto) {
        return mapper.toResponse(
                repository.save(mapper.toEntity(dto))
        );
    }

    @Override
    public ProductVariantSizeResponseDTO getById(String id) {
        return mapper.toResponse(
                repository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Size not found"))
        );
    }

    @Override
    public List<ProductVariantSizeResponseDTO> getByVariantId(String variantId) {
        return repository.findByProductVariantId(variantId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public ProductVariantSizeResponseDTO update(String id, ProductVariantSizeRequestDTO dto) {

        ProductVariantSizeEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Size not found"));

        mapper.update(entity, dto);

        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }
}