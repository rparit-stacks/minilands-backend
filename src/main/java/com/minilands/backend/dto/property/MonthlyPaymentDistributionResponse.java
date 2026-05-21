package com.minilands.backend.dto.property;

import java.math.BigDecimal;
import java.time.Instant;

/** Result of executing a monthly payment: investor + SPV wallet credits. */
public record MonthlyPaymentDistributionResponse(
        String propertyId,
        Instant accrualStart,
        Instant accrualEnd,
        BigDecimal elapsedDays,
        BigDecimal monthlyAmount,
        BigDecimal poolGross,
        BigDecimal platformFee,
        BigDecimal poolNet,
        BigDecimal denominatorShareDays,
        int investorsPaid,
        BigDecimal investorsTotalDistributed,
        BigDecimal spvDistributed,
        // sum of investor + SPV wallet credits
        BigDecimal totalDistributed
) {
}
