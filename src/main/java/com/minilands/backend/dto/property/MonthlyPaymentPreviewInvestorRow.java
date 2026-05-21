package com.minilands.backend.dto.property;

import java.math.BigDecimal;

public record MonthlyPaymentPreviewInvestorRow(
        String holdingId,
        String userId,
        BigDecimal sharesOwned,
        BigDecimal eligibleDays,
        BigDecimal shareDayWeight,
        BigDecimal estimatedPayout
) {
}
