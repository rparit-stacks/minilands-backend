package com.minilands.backend.service.property;

import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.enums.ValuationFrequency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * estimatedSharePrice = baseSharePrice + (baseSharePrice × dailyGrowthRate × daysPassed)
 * where baseSharePrice = totalValue / totalShares,
 * dailyGrowthRate = annualROI / 365 / 100.
 */
@Service
public class SharePriceValuationService {

    private static final int PRICE_SCALE = 4;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal DAYS_PER_YEAR = new BigDecimal("365");

    public BigDecimal getBaseSharePrice(Property property) {
        BigDecimal totalValue = property.getTotalValue();
        Integer totalShares = property.getTotalShares();
        if (totalValue == null || totalShares == null || totalShares < 1) {
            throw new IllegalStateException("Property totalValue and totalShares must be set");
        }
        return totalValue.divide(BigDecimal.valueOf(totalShares), PRICE_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal getEstimatedSharePrice(Property property) {
        return getEstimatedSharePrice(property, Instant.now());
    }

    public BigDecimal getEstimatedSharePrice(Property property, Instant asOf) {
        BigDecimal baseSharePrice = getBaseSharePrice(property);
        BigDecimal annualRoi = property.getAnnualRoi();
        if (annualRoi == null || annualRoi.compareTo(BigDecimal.ZERO) <= 0) {
            return baseSharePrice.setScale(2, RoundingMode.HALF_UP);
        }

        Instant valuationAnchor = property.getLastValuationDate() != null
                ? property.getLastValuationDate()
                : property.getCreatedAt();
        if (valuationAnchor == null) {
            valuationAnchor = asOf;
        }

        long daysPassed = ChronoUnit.DAYS.between(
                valuationAnchor.atZone(ZoneOffset.UTC).toLocalDate(),
                asOf.atZone(ZoneOffset.UTC).toLocalDate());
        if (daysPassed < 0) {
            daysPassed = 0;
        }

        BigDecimal dailyGrowthRate = annualRoi
                .divide(DAYS_PER_YEAR, 10, RoundingMode.HALF_UP)
                .divide(HUNDRED, 10, RoundingMode.HALF_UP);

        BigDecimal growthComponent = baseSharePrice
                .multiply(dailyGrowthRate)
                .multiply(BigDecimal.valueOf(daysPassed));

        return baseSharePrice.add(growthComponent).setScale(2, RoundingMode.HALF_UP);
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
