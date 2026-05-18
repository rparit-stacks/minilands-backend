package com.minilands.backend.dto.investment;

import com.minilands.backend.entity.enums.HoldingStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record HoldingResponse(
        String id,
        String propertyId,
        String propertyName,
        BigDecimal sharesOwned,
        BigDecimal investmentAmount,
        BigDecimal currentValue,
        BigDecimal roiEarned,
        HoldingStatus status,
        Instant entryDate
) {
}
