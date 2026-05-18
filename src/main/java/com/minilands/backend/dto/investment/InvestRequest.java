package com.minilands.backend.dto.investment;

import java.math.BigDecimal;

public record InvestRequest(
        String propertyId,
        BigDecimal amount
) {
}
