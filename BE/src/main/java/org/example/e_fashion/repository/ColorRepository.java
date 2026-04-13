package org.example.e_fashion.repository;

import org.example.e_fashion.entity.BrandEntity;
import org.example.e_fashion.entity.ColorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ColorRepository extends JpaRepository<ColorEntity, String> {
    Optional<ColorEntity> findBySlug(String slug);

    Optional<ColorEntity> findByNameIgnoreCase(String name);

    List<ColorEntity> findByIsActiveTrueOrderByNameAsc();
}