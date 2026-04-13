package org.example.e_fashion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.CategoryRequestDTO;
import org.example.e_fashion.dto.response.CategoryTreeDTO;
import org.example.e_fashion.entity.CategoryEntity;
import org.example.e_fashion.mapper.CategoryMapper;
import org.example.e_fashion.repository.CategoryRepository;
import org.example.e_fashion.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryEntity create(CategoryRequestDTO request) {

        CategoryEntity category = categoryMapper.toEntity(request);

        String slug = generateSlug(request.getName());

        if (request.getParentId() != null) {

            CategoryEntity parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));

            if (categoryRepository.existsBySlugAndParent_Id(slug, parent.getId())) {
                throw new RuntimeException("Category slug already exists in this parent");
            }

            category.setParent(parent);
            slug = generateFullSlug(slug, parent);

        } else {

            if (categoryRepository.existsBySlugAndParentIsNull(slug)) {
                throw new RuntimeException("Root category slug already exists");
            }

            category.setParent(null);
        }

        category.setSlug(slug);

        return categoryRepository.save(category);
    }

    @Override
    public CategoryEntity update(String categoryId, CategoryRequestDTO request) {

        CategoryEntity category = getById(categoryId);
        categoryMapper.updateEntity(category, request);

        if (request.getParentId() != null) {
            if (request.getParentId().equals(categoryId)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }

            CategoryEntity parent = getById(request.getParentId());
            category.setParent(parent);

        } else {
            category.setParent(null);
        }

        return categoryRepository.save(category);
    }

    @Override
    public void delete(String categoryId) {
        categoryRepository.delete(getById(categoryId));
    }

    @Override
    public CategoryEntity getById(String categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    @Override
    public List<CategoryEntity> getAll() {
        return categoryRepository.findAll();
    }

    @Override
    public List<CategoryEntity> getRootCategories() {
        return categoryRepository.findByParentIsNull();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeDTO> getCategoryTree() {

        List<CategoryEntity> roots = categoryRepository.findByParentIsNull();

        return roots.stream()
                .map(categoryMapper::toTree)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryTreeDTO getCategoryTreeById(String id) {

        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        return categoryMapper.toTree(category);
    }

    private String generateSlug(String input) {
        String slug = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug;
    }

    private String generateFullSlug(String slug, CategoryEntity parent) {
        if (parent == null) {
            return slug;
        }
        return parent.getSlug() + "/" + slug;
    }
}