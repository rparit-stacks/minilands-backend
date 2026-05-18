package com.minilands.backend.dto.auth;

import com.minilands.backend.dto.common.PrincipalType;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        String principalId,
        PrincipalType principalType
) {
}
