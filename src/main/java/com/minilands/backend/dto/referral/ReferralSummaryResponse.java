package com.minilands.backend.dto.referral;

import java.math.BigDecimal;

/**
 * Everything the "Refer & Earn" screen needs in one call.
 */
public record ReferralSummaryResponse(
        boolean enabled,
        String referralCode,
        /** Full shareable HTTPS link, e.g. https://openlink.minilands.in/refer/ABC123. */
        String referralLink,
        /** Friends who joined with this user's code (any status). */
        int totalReferrals,
        /** Friends whose reward has actually been credited (completed first investment). */
        int rewardedReferrals,
        /** Lifetime referral earnings credited to this user's wallet. */
        BigDecimal totalEarned,
        String currency,
        /** Per-referral reward at the user's current tier (what the next reward is worth). */
        BigDecimal currentRewardPerReferral,
        /** One-time welcome bonus the referred friend receives. */
        BigDecimal friendBonus,
        /** Marketing copy describing the tier ladder, e.g. "1–5: ₹100 · 6+: ₹200". */
        String tierSummary
) {
}
