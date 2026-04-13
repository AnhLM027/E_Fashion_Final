package org.example.e_fashion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.BrandRequestDTO;
import org.example.e_fashion.entity.BrandEntity;
import org.example.e_fashion.mapper.BrandMapper;
import org.example.e_fashion.repository.BrandRepository;
import org.example.e_fashion.service.BrandService;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Override
    public BrandEntity create(BrandRequestDTO request) {
        String slug = generateSlug(request.getName());

        if (brandRepository.existsBySlug(slug)) {
            throw new RuntimeException("Brand slug already exists");
        }

        BrandEntity brand = brandMapper.toEntity(request);
        brand.setSlug(slug);

        return brandRepository.save(brand);
    }

    @Override
    public List<BrandEntity> getAll() {
        return brandRepository.findAll();
    }

    @Override
    public BrandEntity getById(String id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found"));
    }

    @Override
    public BrandEntity update(String id, BrandRequestDTO request) {

        BrandEntity brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found"));

        if (!brand.getName().equals(request.getName())) {

            String newSlug = generateSlug(request.getName());

            brandRepository.findBySlug(newSlug).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new RuntimeException("Brand slug already exists");
                }
            });

            brand.setSlug(newSlug);
        }

        brand.setName(request.getName());
        brand.setLogoUrl(request.getLogoUrl());

        return brandRepository.save(brand);
    }

    @Override
    public void delete(String id) {
        if (!brandRepository.existsById(id)) {
            throw new RuntimeException("Brand not found");
        }
        brandRepository.deleteById(id);
    }

    // util
    private String generateSlug(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}