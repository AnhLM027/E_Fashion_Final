package org.example.e_fashion.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExtractUserUtils {
    private final JwtTokenUtils jwtTokenUtils;
    private final UserRepository userRepository;

    public UserEntity extract(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new RuntimeException("No cookies found");
        }

        String token = null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("accessToken")) {
                token = cookie.getValue();
                break;
            }
        }
        String username = jwtTokenUtils.extractUsername(token);
        return userRepository
                .findByEmailAndIsActive(username, true)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
