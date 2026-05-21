package com.minilands.backend.dto.property;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Preview of a rent distribution run. {@code denominatorShareDays} is totalShares×elapsedDays (reference).
 * Payouts use only investor weights; {@code spvShareDayWeight} is unsold share-days (reference); {@code spvEstimatedPayout} is always zero.
 * {@code totalShareDayWeight} mirrors denominator for older clients.
 */
public record MonthlyPaymentPreviewResponse(
        String propertyId,
        Instant accrualStart,
        Instant accrualEnd,
        BigDecimal elapsedDays,
        BigDecimal monthlyAmount,
        BigDecimal poolGross,
        BigDecimal platformFee,
        BigDecimal poolNet,
        // totalShares × elapsedDays — denominator for splitting poolNet
        BigDecimal denominatorShareDays,
        // sum of investor (shares × eligibleDays)
        BigDecimal investorShareDaysSum,
        // unsold share-days in window (reference only; not wallet-credited)
        BigDecimal spvShareDayWeight,
        // always zero — full pool goes to investors only
        BigDecimal spvEstimatedPayout,
        @Deprecated
        BigDecimal totalShareDayWeight,
        List<MonthlyPaymentPreviewInvestorRow> investors
) {
}
