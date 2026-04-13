package org.example.e_fashion.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.CategoryRequestDTO;
import org.example.e_fashion.dto.response.CategoryResponseDTO;
import org.example.e_fashion.entity.CategoryEntity;
import org.example.e_fashion.mapper.CategoryMapper;
import org.example.e_fashion.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class CategoryAdminController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> create(@Valid @RequestBody CategoryRequestDTO request) {
        CategoryEntity category = categoryService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(categoryMapper.toResponse(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> update(
            @PathVariable String id,
            @Valid @RequestBody CategoryRequestDTO request) {

        CategoryEntity updatedCategory = categoryService.update(id, request);

        return ResponseEntity.ok(
                categoryMapper.toResponse(updatedCategory)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
