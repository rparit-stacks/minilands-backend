package com.minilands.backend.service.kyc;

import com.minilands.backend.entity.User;
import com.minilands.backend.entity.enums.KycStatus;

/**
 * Shared check for services that require approved KYC (wallet deposits, investments).
 */
public final class KycGuard {

    private KycGuard() {
    }

    public static void requireApproved(User user) {
        if (user.getKycStatus() != KycStatus.APPROVED) {
            throw new IllegalArgumentException("KYC not approved yet. Complete KYC before continuing.");
        }
    }
}
