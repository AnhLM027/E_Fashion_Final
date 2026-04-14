package org.example.e_fashion.controller.client;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.LoginRequestDTO;
import org.example.e_fashion.dto.request.RegisterRequestDTO;
import org.example.e_fashion.dto.request.ResetPasswordDTO;
import org.example.e_fashion.dto.response.LoginResponseDTO;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.entity.enums.RoleEnum;
import org.example.e_fashion.service.AuthService;
import org.example.e_fashion.utils.JwtTokenUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtTokenUtils jwtTokenUtils;

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @RequestBody RegisterRequestDTO registerRequestDTO
    ) {
        authService.register(registerRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/accept")
    public ResponseEntity<?> acceptAccount(@RequestParam String token) {

        authService.acceptAccount(token);

        return ResponseEntity.ok("Account activated");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequestDTO, HttpServletRequest request) {
        LoginResponseDTO response = authService.login(loginRequestDTO);

        boolean isSecure = request.isSecure() || request.getHeader("X-Forwarded-Proto") != null && request.getHeader("X-Forwarded-Proto").equals("https");

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", response.getAccessToken())
                .httpOnly(true)
                .path("/")
                .secure(isSecure)
                .sameSite("Lax")
                .maxAge(jwtTokenUtils.getRemainingExpiration(response.getAccessToken()))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                .httpOnly(true)
                .path("/")
                .secure(isSecure)
                .sameSite("Lax")
                .maxAge(jwtTokenUtils.getRemainingExpiration(response.getRefreshToken()))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of("role", response.getRole()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "refreshToken") String refreshToken, HttpServletRequest request) {

        String username = jwtTokenUtils.extractUsername(refreshToken);
        System.out.println("Username: " + username);

        UserDetails userDetails = authService.loadUserByUsername(username);

        if (!jwtTokenUtils.validateRefreshToken(refreshToken, userDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String newAccessToken = jwtTokenUtils.generateAccessToken(userDetails);

        boolean isSecure = request.isSecure() || request.getHeader("X-Forwarded-Proto") != null && request.getHeader("X-Forwarded-Proto").equals("https");

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", newAccessToken)
                .httpOnly(true)
                .path("/")
                .secure(isSecure)
                .sameSite("Lax")
                .maxAge(jwtTokenUtils.getRemainingExpiration(newAccessToken))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        request.getSession().invalidate();

        boolean isSecure = request.isSecure() || request.getHeader("X-Forwarded-Proto") != null && request.getHeader("X-Forwarded-Proto").equals("https");

        ResponseCookie deleteAccess = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .path("/")
                .secure(isSecure)
                .sameSite("Lax")
                .maxAge(0)
                .build();

        ResponseCookie deleteRefresh = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/")
                .secure(isSecure)
                .sameSite("Lax")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteAccess.toString())
                .header(HttpHeaders.SET_COOKIE, deleteRefresh.toString())
                .build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestParam String email
    ) throws MessagingException {
        authService.forgotPassword(email);
        return ResponseEntity.ok("Reset link sent");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody ResetPasswordDTO request
    ){
        authService.resetPassword(
                request.getToken(),
                request.getNewPassword()
        );

        return ResponseEntity.ok("Password updated");
    }
}
