package org.example.e_fashion.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.BrandRequestDTO;
import org.example.e_fashion.dto.response.BrandResponseDTO;
import org.example.e_fashion.mapper.BrandMapper;
import org.example.e_fashion.service.BrandService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
public class BrandAdminController {

    private final BrandService brandService;
    private final BrandMapper brandMapper;

    @PostMapping
    public ResponseEntity<BrandResponseDTO> create(
            @Valid @RequestBody BrandRequestDTO request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(brandMapper.toResponse(brandService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BrandResponseDTO> update(
            @PathVariable String id,
            @Valid @RequestBody BrandRequestDTO request) {

        return ResponseEntity.ok(
                brandMapper.toResponse(brandService.update(id, request))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        brandService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
