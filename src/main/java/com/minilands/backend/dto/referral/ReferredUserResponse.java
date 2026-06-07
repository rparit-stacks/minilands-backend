package com.minilands.backend.dto.referral;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One referred friend, shown in the referral list on the app.
 */
public record ReferredUserResponse(
        /** Masked display name / email (we never leak the friend's full contact). */
        String name,
        /** "PENDING" until they complete their first investment, then "REWARDED". */
        String status,
        Instant joinedAt,
        /** Reward this user earned for this particular friend (0 until rewarded). */
        BigDecimal rewardEarned
) {
}
