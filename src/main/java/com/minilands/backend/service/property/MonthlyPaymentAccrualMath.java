package com.minilands.backend.service.property;

import com.minilands.backend.entity.enums.DistributionFrequency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * Accrual window + proration helpers for monthly payment (30-day standard month, fractional days from wall clock).
 */
public final class MonthlyPaymentAccrualMath {

    /** Fixed denominator for prorating {@code monthlyAmount} over elapsed time (product choice). */
    public static final BigDecimal STANDARD_MONTH_DAYS = new BigDecimal("30");

    private MonthlyPaymentAccrualMath() {
    }

    public static BigDecimal elapsedDays(Instant accrualStart, Instant accrualEnd) {
        if (!accrualEnd.isAfter(accrualStart)) {
            throw new IllegalArgumentException("accrualEnd must be after accrualStart");
        }
        long minutes = Duration.between(accrualStart, accrualEnd).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(24 * 60L), 10, RoundingMode.HALF_UP);
    }

    public static BigDecimal poolGross(BigDecimal monthlyAmount, Instant accrualStart, Instant accrualEnd) {
        BigDecimal elapsed = elapsedDays(accrualStart, accrualEnd);
        return monthlyAmount.multiply(elapsed).divide(STANDARD_MONTH_DAYS, 2, RoundingMode.HALF_UP);
    }

    /**
     * Minimum days an investor must have held shares (since their entryDate) at the time of
     * accrualEnd to be eligible for rent in this run. Based on payout frequency — an investor
     * who bought shares less than one full cycle ago is not yet eligible.
     */
    public static BigDecimal minEligibleDays(DistributionFrequency frequency) {
        if (frequency == null) {
            return STANDARD_MONTH_DAYS; // default: monthly
        }
        return switch (frequency) {
            case MONTHLY -> new BigDecimal("30");
            case QUARTERLY -> new BigDecimal("90");
            case SEMI_ANNUAL -> new BigDecimal("180");
            case ANNUAL -> new BigDecimal("365");
        };
    }

    /**
     * Overlap of holding period {@code [entry, holdingEnd)} with window {@code [windowStart, windowEnd)},
     * returned as fractional days (same minute→day scale as {@link #elapsedDays}).
     */
    public static BigDecimal eligibleOverlapDays(
            Instant entryDate,
            Instant holdingEndExclusive,
            Instant windowStart,
            Instant windowEnd) {
        if (entryDate == null || !windowEnd.isAfter(windowStart)) {
            return BigDecimal.ZERO;
        }
        Instant overlapStart = entryDate.isAfter(windowStart) ? entryDate : windowStart;
        Instant overlapEnd = holdingEndExclusive.isBefore(windowEnd) ? holdingEndExclusive : windowEnd;
        if (!overlapEnd.isAfter(overlapStart)) {
            return BigDecimal.ZERO;
        }
        long minutes = Duration.between(overlapStart, overlapEnd).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(24 * 60L), 10, RoundingMode.HALF_UP);
    }
}
