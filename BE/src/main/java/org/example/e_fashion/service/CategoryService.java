package org.example.e_fashion.service;

import org.example.e_fashion.dto.request.CategoryRequestDTO;
import org.example.e_fashion.dto.response.CategoryTreeDTO;
import org.example.e_fashion.entity.CategoryEntity;

import java.util.List;

public interface CategoryService {

    CategoryEntity create(CategoryRequestDTO request);

    CategoryEntity update(String categoryId, CategoryRequestDTO request);

    void delete(String categoryId);

    CategoryEntity getById(String categoryId);

    List<CategoryEntity> getAll();

    List<CategoryEntity> getRootCategories();

    List<CategoryTreeDTO> getCategoryTree();

    CategoryTreeDTO getCategoryTreeById(String id);
}