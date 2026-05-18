package com.minilands.backend.service.auth;

import com.minilands.backend.dto.auth.AuthResponse;
import com.minilands.backend.dto.auth.LoginRequest;
import com.minilands.backend.dto.auth.RefreshTokenRequest;

/**
 * Admin authentication — separate from {@link UserAuthService} (ISP, separate admins collection).
 */
public interface AdminAuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String adminId);
}
