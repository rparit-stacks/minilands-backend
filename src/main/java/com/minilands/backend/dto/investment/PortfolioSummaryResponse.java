package com.minilands.backend.dto.investment;

import java.math.BigDecimal;

public record PortfolioSummaryResponse(
        int totalHoldings,
        int activeHoldings,
        BigDecimal totalInvested,
        BigDecimal currentPortfolioValue,
        BigDecimal totalRoiEarned,
        BigDecimal unrealizedGain,
        BigDecimal unrealizedGainPercent
) {
}
