package com.minilands.backend.service.auth;

import com.minilands.backend.dto.auth.AuthResponse;
import com.minilands.backend.dto.auth.GoogleAuthRequest;
import com.minilands.backend.dto.auth.RefreshTokenRequest;
import com.minilands.backend.dto.auth.SendOtpRequest;
import com.minilands.backend.dto.auth.VerifyOtpRequest;

public interface UserAuthService {

    void sendEmailOtp(SendOtpRequest request);

    AuthResponse verifyEmailOtp(VerifyOtpRequest request);

    AuthResponse authenticateWithGoogle(GoogleAuthRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String userId);
}
