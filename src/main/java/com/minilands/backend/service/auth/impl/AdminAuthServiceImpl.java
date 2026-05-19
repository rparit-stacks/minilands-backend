package com.minilands.backend.service.auth.impl;

import com.minilands.backend.config.JwtProperties;
import com.minilands.backend.dto.auth.AuthResponse;
import com.minilands.backend.dto.auth.LoginRequest;
import com.minilands.backend.dto.auth.RefreshTokenRequest;
import com.minilands.backend.dto.common.PrincipalType;
import com.minilands.backend.entity.Admin;
import com.minilands.backend.entity.AdminRefreshToken;
import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.exception.AuthException;
import com.minilands.backend.repository.AdminRefreshTokenRepository;
import com.minilands.backend.repository.AdminRepository;
import com.minilands.backend.security.JwtService;
import com.minilands.backend.security.TokenHashService;
import com.minilands.backend.service.auth.AdminAuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminRepository adminRepository;
    private final AdminRefreshTokenRepository adminRefreshTokenRepository;
    private final JwtService jwtService;
    private final TokenHashService tokenHashService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    public AdminAuthServiceImpl(
            AdminRepository adminRepository,
            AdminRefreshTokenRepository adminRefreshTokenRepository,
            JwtService jwtService,
            TokenHashService tokenHashService,
            PasswordEncoder passwordEncoder,
            JwtProperties jwtProperties) {
        this.adminRepository = adminRepository;
        this.adminRefreshTokenRepository = adminRefreshTokenRepository;
        this.jwtService = jwtService;
        this.tokenHashService = tokenHashService;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new AuthException("Invalid email or password");
        }

        if (admin.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AuthException("Account is not active");
        }

        admin.setLastLoginAt(Instant.now());
        admin.setUpdatedAt(Instant.now());
        adminRepository.save(admin);

        return issueTokens(admin);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String rawRefreshToken = request.refreshToken();
        jwtService.validateRefreshToken(rawRefreshToken, PrincipalType.ADMIN);

        String adminId = jwtService.extractUserId(rawRefreshToken);
        AdminRefreshToken stored = findActiveRefreshToken(adminId, rawRefreshToken);

        stored.setRevoked(true);
        adminRefreshTokenRepository.save(stored);

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new AuthException("Admin not found"));

        if (admin.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AuthException("Account is not active");
        }

        return issueTokens(admin);
    }

    @Override
    public void logout(String adminId) {
        adminRefreshTokenRepository.deleteByAdminId(adminId);
    }

    private AuthResponse issueTokens(Admin admin) {
        String accessToken = jwtService.generateAccessToken(
                admin.getId(), admin.getEmail(), PrincipalType.ADMIN);
        String refreshToken = jwtService.generateRefreshToken(
                admin.getId(), admin.getEmail(), PrincipalType.ADMIN);
        persistRefreshToken(admin.getId(), refreshToken);

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtService.getAccessExpirationSeconds(),
                admin.getId(),
                PrincipalType.ADMIN);
    }

    private void persistRefreshToken(String adminId, String rawRefreshToken) {
        AdminRefreshToken refreshToken = new AdminRefreshToken();
        refreshToken.setAdminId(adminId);
        refreshToken.setTokenHash(tokenHashService.hash(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs()));
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(Instant.now());
        adminRefreshTokenRepository.save(refreshToken);
    }

    private AdminRefreshToken findActiveRefreshToken(String adminId, String rawRefreshToken) {
        return adminRefreshTokenRepository.findByAdminId(adminId).stream()
                .filter(token -> !token.isRevoked())
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .filter(token -> tokenHashService.matches(rawRefreshToken, token.getTokenHash()))
                .findFirst()
                .orElseThrow(() -> new AuthException("Invalid refresh token"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
