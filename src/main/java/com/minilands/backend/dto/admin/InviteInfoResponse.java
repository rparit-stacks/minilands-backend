package com.minilands.backend.dto.admin;

import com.minilands.backend.entity.enums.AdminRole;

import java.time.Instant;

/** Public response for the setup page — confirms the token is valid and shows context. */
public record InviteInfoResponse(
        String email,
        AdminRole role,
        Instant expiresAt
) {
}
