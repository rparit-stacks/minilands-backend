package com.minilands.backend.service.property;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fractional listing math: sharePrice = totalTarget ÷ totalShares.
 */
public final class PropertyInvestmentMath {

    private static final int SHARE_PRICE_SCALE = 2;
    private static final BigDecimal TOLERANCE_RATIO = new BigDecimal("0.01");

    private PropertyInvestmentMath() {
    }

    public static BigDecimal deriveSharePrice(BigDecimal totalValue, int totalShares) {
        if (totalValue == null || totalShares < 1) {
            throw new IllegalArgumentException("totalValue and totalShares are required to calculate sharePrice");
        }
        return totalValue.divide(BigDecimal.valueOf(totalShares), SHARE_PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Uses derived price when {@code requestedSharePrice} is null; otherwise validates it matches derived value.
     */
    public static BigDecimal resolveSharePrice(
            BigDecimal totalValue,
            int totalShares,
            BigDecimal requestedSharePrice) {
        BigDecimal derived = deriveSharePrice(totalValue, totalShares);
        if (requestedSharePrice == null) {
            return derived;
        }

        BigDecimal diff = requestedSharePrice.subtract(derived).abs();
        BigDecimal tolerance = derived.multiply(TOLERANCE_RATIO).max(new BigDecimal("0.01"));
        if (diff.compareTo(tolerance) > 0) {
            throw new IllegalArgumentException(
                    "sharePrice is auto-calculated as totalTarget ÷ totalShares ("
                            + derived
                            + "). Omit sharePrice from the request.");
        }
        return derived;
    }
}
