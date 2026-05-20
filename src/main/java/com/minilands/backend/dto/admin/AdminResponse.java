package com.minilands.backend.dto.admin;

import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.entity.enums.AdminRole;

import java.time.Instant;

public record AdminResponse(
        String id,
        String email,
        String name,
        AdminRole role,
        AccountStatus accountStatus,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
}
