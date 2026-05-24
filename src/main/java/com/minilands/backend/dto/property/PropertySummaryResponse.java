package com.minilands.backend.dto.property;

import com.minilands.backend.entity.enums.DistributionFrequency;
import com.minilands.backend.entity.enums.PropertyStatus;
import com.minilands.backend.entity.enums.PropertyType;

import java.math.BigDecimal;
import java.time.Instant;

public record PropertySummaryResponse(
        String id,
        String slug,
        String name,
        String tagline,
        String shortDescription,
        PropertyType propertyType,
        String city,
        String state,
        String country,
        String locationDisplay,
        String primaryImageUrl,
        String currency,
        BigDecimal totalTarget,
        Integer totalShares,
        BigDecimal sharePrice,
        BigDecimal currentPrice,
        BigDecimal minInvestmentAmount,
        BigDecimal annualRoi,
        BigDecimal monthlyRoi,
        BigDecimal projectedAnnualYield,
        PropertyStatus status,
        boolean featured,
        Integer currentInvestors,
        BigDecimal totalRaised,
        BigDecimal fundingProgressPercent,
        BigDecimal sharesRemaining,
        boolean fundingOpen,
        Instant fundingDeadline,
        Instant publishedAt,
        BigDecimal rentalYieldPercent,
        BigDecimal monthlyRent,
        Integer rentPlatformFeePercent,
        BigDecimal marketplaceFeePercent,
        DistributionFrequency distributionFrequency,
        Instant lastMonthlyPaymentDistributedAt
) {
}
