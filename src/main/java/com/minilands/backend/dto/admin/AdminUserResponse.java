package com.minilands.backend.dto.admin;

import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.entity.enums.AuthProvider;
import com.minilands.backend.entity.enums.KycStatus;

import java.time.Instant;

public record AdminUserResponse(
        String id,
        String email,
        String name,
        String phone,
        String profilePictureUrl,
        AuthProvider authProvider,
        KycStatus kycStatus,
        AccountStatus accountStatus,
        Instant emailVerifiedAt,
        Instant kycVerifiedAt,
        String kycRejectionNote,
        Instant createdAt,
        Instant updatedAt
) {
}
