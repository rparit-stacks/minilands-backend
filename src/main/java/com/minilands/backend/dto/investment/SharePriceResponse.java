package com.minilands.backend.dto.investment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SharePriceResponse(
        String propertyId,
        String propertyName,
        BigDecimal baseSharePrice,
        BigDecimal estimatedSharePrice,
        BigDecimal expectedAnnualRoi,
        Instant lastValuationDate,
        LocalDate nextValuationDate
) {
}
