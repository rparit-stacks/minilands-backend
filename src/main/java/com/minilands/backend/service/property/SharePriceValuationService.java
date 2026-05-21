package com.minilands.backend.service.property;

import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.enums.ValuationFrequency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Investor share value follows the latest building valuation: one share = total property value ÷ total shares.
 * No separate manual “growth bump” on top — admin valuation updates drive per-share value; annual % ROI is
 * derived when valuation is saved (see {@link com.minilands.backend.service.property.impl.AdminPropertyValuationServiceImpl}).
 */
@Service
public class SharePriceValuationService {

    private static final int PRICE_SCALE = 4;

    public BigDecimal getBaseSharePrice(Property property) {
        BigDecimal totalValue = property.getTotalValue();
        Integer totalShares = property.getTotalShares();
        if (totalValue == null || totalShares == null || totalShares < 1) {
            throw new IllegalStateException("Property totalValue and totalShares must be set");
        }
        return totalValue.divide(BigDecimal.valueOf(totalShares), PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /** Per-share value from latest valuation (totalValue ÷ totalShares). */
    public BigDecimal getEstimatedSharePrice(Property property) {
        return getBaseSharePrice(property).setScale(2, RoundingMode.HALF_UP);
    }

    /** Same as {@link #getEstimatedSharePrice(Property)} — kept for call sites that pass an instant. */
    public BigDecimal getEstimatedSharePrice(Property property, Instant asOf) {
        return getEstimatedSharePrice(property);
    }

    public LocalDate getNextValuationDate(Property property) {
        Instant last = property.getLastValuationDate() != null
                ? property.getLastValuationDate()
                : property.getCreatedAt();
        if (last == null) {
            return LocalDate.now(ZoneOffset.UTC);
        }
        LocalDate base = last.atZone(ZoneOffset.UTC).toLocalDate();
        ValuationFrequency frequency = property.getValuationFrequency() != null
                ? property.getValuationFrequency()
                : ValuationFrequency.QUARTERLY;
        return switch (frequency) {
            case MONTHLY -> base.plusMonths(1);
            case QUARTERLY -> base.plusMonths(3);
            case SEMI_ANNUAL -> base.plusMonths(6);
            case ANNUAL -> base.plusYears(1);
        };
    }
}
