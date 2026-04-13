package org.example.e_fashion.repository;

import org.example.e_fashion.entity.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository
        extends JpaRepository<CouponEntity, String> {

    Optional<CouponEntity> findByCode(String code);
}