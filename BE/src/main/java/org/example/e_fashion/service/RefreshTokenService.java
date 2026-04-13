package org.example.e_fashion.service;

import org.example.e_fashion.entity.RefreshTokenEntity;
import org.example.e_fashion.entity.UserEntity;

public interface RefreshTokenService {
    RefreshTokenEntity createRefreshToken(UserEntity user);

    RefreshTokenEntity verify(String token);

    void revoke(String token);

    void revokeAllUserTokens(String userId);
}
