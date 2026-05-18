package com.minilands.backend.dto.wallet;

import java.math.BigDecimal;

public record WithdrawalRequest(
        BigDecimal amount,
        String bankAccountId
) {
}
