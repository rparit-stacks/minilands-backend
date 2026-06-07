package com.minilands.backend.dto.referral;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * A single reward bracket. {@code maxReferrals == null} means the top,
 * open-ended tier ("and above").
 */
public record ReferralTierDto(
        @Min(1) int minReferrals,
        Integer maxReferrals,
        @NotNull @PositiveOrZero BigDecimal rewardPerReferral
) {
}
