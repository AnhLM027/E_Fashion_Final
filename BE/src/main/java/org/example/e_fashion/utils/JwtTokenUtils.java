package org.example.e_fashion.utils;

import org.example.e_fashion.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtTokenUtils {
    @Value("${jwt.access_expiration}")
    private long ACCESS_EXPIRATION;
    @Value("${jwt.refresh_expiration}")
    private long REFRESH_EXPIRATION;
    @Value("${jwt.secretKey}")
    private String secretKey;

    public String generateVerifyToken(UserEntity user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .claim("type", "accept")
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24h
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateAccessToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION * 1000))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(UserEntity user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION * 1000))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public int getRemainingExpiration(String token) {
        Date expiration = this.getClaimsFromToken(token).getExpiration();
        long remainingMillis = expiration.getTime() - System.currentTimeMillis();
        return (int)Math.max(remainingMillis / 1000, 0);
    }

    public <T> T extractClaimsFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = this.getClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    public boolean isTokenExpired(String token) {
        final Date expiration = this.getClaimsFromToken(token).getExpiration();
        return expiration.before(new Date());
    }

    public String extractUsername(String token) {
        return extractClaimsFromToken(token, Claims::getSubject);
    }

    public boolean validateVerifyToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        String type = getClaimsFromToken(token).get("type", String.class);

        return username.equals(userDetails.getUsername())
                && "accept".equals(type)
                && !isTokenExpired(token);
    }

    public boolean validateAccessToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        String type = getClaimsFromToken(token).get("type", String.class);

        return username.equals(userDetails.getUsername())
                && "access".equals(type)
                && !isTokenExpired(token);
    }

    public boolean validateRefreshToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        String type = getClaimsFromToken(token).get("type", String.class);

        return username.equals(userDetails.getUsername())
                && "refresh".equals(type)
                && !isTokenExpired(token);
    }

    public String generateResetToken(UserEntity user) {

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("type", "reset")
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(System.currentTimeMillis() + 15 * 60 * 1000)
                )
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateResetToken(String token, UserDetails userDetails) {

        String username = extractUsername(token);
        String type = getClaimsFromToken(token).get("type", String.class);

        return username.equals(userDetails.getUsername())
                && "reset".equals(type)
                && !isTokenExpired(token);
    }
}