package com.minilands.backend.dto.user;

import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.entity.enums.AuthProvider;
import com.minilands.backend.entity.enums.KycStatus;

import java.time.Instant;

public record UserProfileResponse(
        String id,
        String email,
        String name,
        String phone,
        String profilePictureUrl,
        AuthProvider authProvider,
        KycStatus kycStatus,
        AccountStatus accountStatus,
        Instant emailVerifiedAt,
        Instant createdAt,
        boolean onboardingCompleted,
        String referralCode
) {
}
