package com.minilands.backend.dto.investment;

import com.minilands.backend.entity.enums.HoldingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record HoldingDetailResponse(
        String id,
        String propertyId,
        String propertyName,
        BigDecimal sharesOwned,
        BigDecimal costBasis,
        BigDecimal currentSharePrice,
        BigDecimal expectedAnnualRoi,
        Instant lastValuationDate,
        LocalDate nextValuationDate,
        BigDecimal currentInvestmentValue,
        BigDecimal rentalEarnings,
        BigDecimal profitOrLoss,
        HoldingStatus status,
        Instant entryDate
) {
}
