package com.minilands.backend.dto.wallet;

import com.minilands.backend.entity.enums.WithdrawalStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record WithdrawalResponse(
        String id,
        String userId,
        BigDecimal amount,
        WithdrawalStatus status,
        String bankAccountId,
        String adminNote,
        Instant processedAt,
        Instant createdAt
) {
}
