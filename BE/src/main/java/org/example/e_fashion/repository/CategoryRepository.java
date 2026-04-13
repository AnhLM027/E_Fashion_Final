package org.example.e_fashion.repository;

import org.example.e_fashion.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, String> {
    @Query("""
        SELECT c FROM CategoryEntity c
        WHERE c.slug LIKE CONCAT(:slug, '%')
    """)
    List<CategoryEntity> findAllBySlugStartingWith(String slug);

    Optional<CategoryEntity> findBySlug(String slug);

    boolean existsByName(String name);

    List<CategoryEntity> findByParentIsNull();

    boolean existsBySlugAndParent_Id(String slug, String parentId);

    boolean existsBySlugAndParentIsNull(String slug);

    List<CategoryEntity> findByParent_Id(String parentId);

    List<CategoryEntity> findByIsActiveTrue();

    boolean existsBySlug(String slug);
}
