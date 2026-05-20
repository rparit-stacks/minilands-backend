package com.minilands.backend.dto.investment;

import java.math.BigDecimal;
import java.time.Instant;

public record RoiEarningResponse(
        String id,
        String roiDistributionId,
        BigDecimal amount,
        BigDecimal roiPercentage,
        Instant earnedOnDate,
        Instant createdAt
) {
}
