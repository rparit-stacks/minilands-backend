package com.minilands.backend.dto.wallet;

import java.math.BigDecimal;

public record WalletBalanceResponse(
        String walletId,
        String userId,
        BigDecimal balance,
        BigDecimal pendingWithdrawals,
        BigDecimal availableBalance,
        String currency
) {
}
