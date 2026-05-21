package com.minilands.backend.dto.property;

import java.math.BigDecimal;
import java.time.Instant;

public record MonthlyPaymentHistoryItem(
        String id,
        Instant accrualStart,
        Instant accrualEnd,
        BigDecimal poolGross,
        BigDecimal poolNet,
        BigDecimal totalDistributed,
        BigDecimal spvDistributed,
        int investorsPaid,
        Instant createdAt
) {
}
