package com.minilands.backend.dto.admin;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminWalletRowResponse(
        String walletId,
        String userId,
        String userEmail,
        String userName,
        BigDecimal balance,
        String currency,
        Instant updatedAt
) {
}
