package org.example.e_fashion.repository;

import org.example.e_fashion.entity.BrandEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrandRepository extends JpaRepository<BrandEntity, String> {

    boolean existsBySlug(String slug);

    Optional<BrandEntity> findBySlug(String newSlug);
}