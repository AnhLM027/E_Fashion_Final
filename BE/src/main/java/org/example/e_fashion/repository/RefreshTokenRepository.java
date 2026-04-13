package org.example.e_fashion.repository;

import org.example.e_fashion.entity.RefreshTokenEntity;
import org.example.e_fashion.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {
    Optional<RefreshTokenEntity> findByTokenAndIsRevokedFalse(String token);

    void deleteByUser(UserEntity user);
}
