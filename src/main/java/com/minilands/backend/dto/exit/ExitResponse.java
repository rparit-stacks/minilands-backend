package com.minilands.backend.dto.exit;

import java.math.BigDecimal;

public record ExitResponse(
        String holdingId,
        BigDecimal exitAmount,
        String transactionId
) {
}
