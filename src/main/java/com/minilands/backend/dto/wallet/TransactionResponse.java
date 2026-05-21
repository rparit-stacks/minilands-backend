package com.minilands.backend.dto.wallet;

import com.minilands.backend.entity.enums.TransactionStatus;
import com.minilands.backend.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        String id,
        TransactionType type,
        BigDecimal amount,
        TransactionStatus status,
        String description,
        Instant createdAt,
        String referenceId
) {
}
