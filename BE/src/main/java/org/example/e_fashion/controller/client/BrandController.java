package org.example.e_fashion.controller.client;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.BrandRequestDTO;
import org.example.e_fashion.dto.response.BrandResponseDTO;
import org.example.e_fashion.mapper.BrandMapper;
import org.example.e_fashion.service.BrandService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;
    private final BrandMapper brandMapper;

    @GetMapping
    public List<BrandResponseDTO> getAll() {
        return brandService.getAll()
                .stream()
                .map(brandMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public BrandResponseDTO getById(@PathVariable String id) {
        return brandMapper.toResponse(brandService.getById(id));
    }
}