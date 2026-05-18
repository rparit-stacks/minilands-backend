package com.minilands.backend.dto.property;

import com.minilands.backend.entity.enums.PropertyStatus;

import java.math.BigDecimal;
import java.util.List;

public record PropertyDetailResponse(
        String id,
        String name,
        String description,
        String location,
        BigDecimal totalTarget,
        Integer totalShares,
        BigDecimal sharePrice,
        BigDecimal currentPrice,
        BigDecimal annualRoi,
        BigDecimal monthlyRoi,
        PropertyStatus status,
        Integer currentInvestors,
        BigDecimal totalRaised,
        List<String> mediaUrls
) {
}
