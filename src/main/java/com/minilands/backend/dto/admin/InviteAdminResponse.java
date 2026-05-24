package com.minilands.backend.dto.admin;

import com.minilands.backend.entity.enums.AdminRole;

import java.time.Instant;

/**
 * Returned to the inviter when an invite is created. {@code setupUrl} is also embedded in the
 * email; we surface it in the API response too so admins can copy/share it manually as a fallback.
 */
public record InviteAdminResponse(
        String email,
        AdminRole role,
        String setupUrl,
        Instant expiresAt,
        boolean emailSent
) {
}
