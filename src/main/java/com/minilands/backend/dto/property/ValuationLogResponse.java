package com.minilands.backend.dto.property;

import java.math.BigDecimal;
import java.time.Instant;

public record ValuationLogResponse(
        String id,
        BigDecimal previousTotalValue,
        BigDecimal newTotalValue,
        BigDecimal previousSharePrice,
        BigDecimal newSharePrice,
        String updatedByAdminId,
        String note,
        Instant valuedAt
) {
}
