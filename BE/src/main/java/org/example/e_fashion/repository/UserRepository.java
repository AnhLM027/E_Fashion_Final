package org.example.e_fashion.repository;

import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.entity.enums.RoleEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.lang.ScopedValue;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByEmailAndIsActive(String username, Boolean isActive);
    Page<UserEntity> findByEmailContainingIgnoreCase(
            String email,
            Pageable pageable
    );

    Page<UserEntity> findByRole(
            RoleEnum role,
            Pageable pageable
    );

    Page<UserEntity> findByIsActive(
            Boolean isActive,
            Pageable pageable
    );

    boolean existsByEmail(String email);
    List<UserEntity> findByRole(RoleEnum roleName);

    Optional<UserEntity> findByEmail(String email);
}
