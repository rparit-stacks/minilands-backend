package com.minilands.backend.dto.property;

import com.minilands.backend.entity.enums.PropertyStatus;

import java.math.BigDecimal;

public record PropertySummaryResponse(
        String id,
        String name,
        String location,
        BigDecimal sharePrice,
        BigDecimal currentPrice,
        BigDecimal monthlyRoi,
        PropertyStatus status,
        Integer currentInvestors,
        BigDecimal totalRaised
) {
}
