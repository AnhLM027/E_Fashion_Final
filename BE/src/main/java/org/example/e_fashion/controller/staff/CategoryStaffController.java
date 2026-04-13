package org.example.e_fashion.controller.staff;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.response.CategoryResponseDTO;
import org.example.e_fashion.dto.response.CategoryTreeDTO;
import org.example.e_fashion.mapper.CategoryMapper;
import org.example.e_fashion.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/categories")
@RequiredArgsConstructor
public class CategoryStaffController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    @GetMapping
    public List<CategoryResponseDTO> getAll() {
        return categoryService.getAll()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public CategoryResponseDTO getById(@PathVariable String id) {
        return categoryMapper.toResponse(categoryService.getById(id));
    }

    @GetMapping("/root")
    public List<CategoryResponseDTO> getRootCategories() {
        return categoryService.getRootCategories()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @GetMapping("/tree")
    public List<CategoryTreeDTO> getCategoryTree() {
        return categoryService.getCategoryTree();
    }

    @GetMapping("/tree/{id}")
    public CategoryTreeDTO getCategoryTreeById(@PathVariable String id) {
        return categoryService.getCategoryTreeById(id);
    }
}
