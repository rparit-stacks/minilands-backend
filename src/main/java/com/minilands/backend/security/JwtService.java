package com.minilands.backend.security;

import com.minilands.backend.config.JwtProperties;
import com.minilands.backend.dto.common.PrincipalType;
import com.minilands.backend.entity.enums.JwtTokenType;
import com.minilands.backend.exception.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String principalId, String email) {
        return generateAccessToken(principalId, email, PrincipalType.INVESTOR);
    }

    public String generateAccessToken(String principalId, String email, PrincipalType principalType) {
        return buildToken(principalId, email, JwtTokenType.ACCESS, jwtProperties.getAccessExpirationMs(), principalType);
    }

    public String generateRefreshToken(String principalId, String email) {
        return generateRefreshToken(principalId, email, PrincipalType.INVESTOR);
    }

    public String generateRefreshToken(String principalId, String email, PrincipalType principalType) {
        return buildToken(principalId, email, JwtTokenType.REFRESH, jwtProperties.getRefreshExpirationMs(), principalType);
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ex) {
            throw new AuthException("Invalid or expired token");
        }
    }

    public String extractUserId(String token) {
        return parseToken(token).getSubject();
    }

    public JwtTokenType extractTokenType(String token) {
        String type = parseToken(token).get("type", String.class);
        return JwtTokenType.valueOf(type);
    }

    public PrincipalType extractPrincipalType(String token) {
        String principalType = parseToken(token).get("principalType", String.class);
        return PrincipalType.valueOf(principalType);
    }

    public void validateAccessToken(String token) {
        validateAccessToken(token, PrincipalType.INVESTOR);
    }

    public void validateAccessToken(String token, PrincipalType expectedPrincipalType) {
        Claims claims = parseToken(token);
        if (!JwtTokenType.ACCESS.name().equals(claims.get("type", String.class))) {
            throw new AuthException("Invalid access token");
        }
        if (!expectedPrincipalType.name().equals(claims.get("principalType", String.class))) {
            throw new AuthException("Invalid token principal");
        }
    }

    public void validateRefreshToken(String token) {
        validateRefreshToken(token, PrincipalType.INVESTOR);
    }

    public void validateRefreshToken(String token, PrincipalType expectedPrincipalType) {
        Claims claims = parseToken(token);
        if (!JwtTokenType.REFRESH.name().equals(claims.get("type", String.class))) {
            throw new AuthException("Invalid refresh token");
        }
        if (!expectedPrincipalType.name().equals(claims.get("principalType", String.class))) {
            throw new AuthException("Invalid token principal");
        }
    }

    public long getAccessExpirationSeconds() {
        return jwtProperties.getAccessExpirationMs() / 1000;
    }

    private String buildToken(String principalId, String email, JwtTokenType type, long expirationMs,
                            PrincipalType principalType) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(principalId)
                .claims(Map.of(
                        "email", email,
                        "type", type.name(),
                        "principalType", principalType.name()
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(secretKey)
                .compact();
    }
}
