package org.example.e_fashion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.entity.RefreshTokenEntity;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.repository.RefreshTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl {

    private final RefreshTokenRepository refreshTokenRepository;
    private static final int REFRESH_EXPIRE_DAYS = 7;

    public RefreshTokenEntity create(UserEntity user, String token) {

        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_EXPIRE_DAYS))
                .isRevoked(false)
                .build();

        return refreshTokenRepository.save(entity);
    }

    public RefreshTokenEntity validate(String token) {

        RefreshTokenEntity entity = refreshTokenRepository
                .findByTokenAndIsRevokedFalse(token)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid refresh token"));

        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token expired");
        }

        return entity;
    }

    public void revoke(String token) {
        refreshTokenRepository.findByTokenAndIsRevokedFalse(token)
                .ifPresent(entity -> {
                    entity.setIsRevoked(true);
                    refreshTokenRepository.save(entity);
                });
    }

    public void revokeAllByUser(UserEntity user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
