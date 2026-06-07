package com.minilands.backend.dto.referral;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Admin read/write view of the referral programme configuration. Used by the
 * admin dashboard for both GET and PUT.
 */
public record ReferralSettingsDto(
        boolean enabled,
        @NotNull @Valid List<ReferralTierDto> tiers,
        @NotNull @PositiveOrZero BigDecimal friendBonus,
        String currency,
        Instant updatedAt
) {
}
