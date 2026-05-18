package com.minilands.backend.dto.wallet;

import java.math.BigDecimal;

public record DepositRequest(
        BigDecimal amount
) {
}
