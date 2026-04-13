package org.example.e_fashion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ColorRequestDTO;
import org.example.e_fashion.dto.response.ColorResponseDTO;
import org.example.e_fashion.entity.ColorEntity;
import org.example.e_fashion.mapper.ColorMapper;
import org.example.e_fashion.repository.ColorRepository;
import org.example.e_fashion.service.ColorService;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ColorServiceImpl implements ColorService {

    private final ColorRepository repository;
    private final ColorMapper mapper;

    @Override
    public ColorResponseDTO create(ColorRequestDTO request) {

        repository.findByNameIgnoreCase(request.getName())
                .ifPresent(c -> {
                    throw new RuntimeException("Color already exists");
                });

        ColorEntity entity = new ColorEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName(request.getName());
        entity.setCode(request.getCode());
        entity.setSlug(generateSlug(request.getName()));
        entity.setIsActive(true);

        repository.save(entity);

        return mapper.toResponse(entity);
    }

    @Override
    public ColorResponseDTO update(String id, ColorRequestDTO request) {

        ColorEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Color not found"));

        entity.setName(request.getName());
        entity.setCode(request.getCode());
        entity.setSlug(generateSlug(request.getName()));

        repository.save(entity);

        return mapper.toResponse(entity);
    }

    @Override
    public void delete(String id) {

        ColorEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Color not found"));

        entity.setIsActive(false);
        repository.save(entity);
    }

    @Override
    public List<ColorResponseDTO> getAllActive() {

        return repository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    private String generateSlug(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}