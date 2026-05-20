package com.minilands.backend.dto.payment;

import com.minilands.backend.entity.enums.DepositStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminDepositResponse(
        String id,
        String userId,
        BigDecimal amount,
        DepositStatus status,
        String razorpayOrderId,
        String razorpayPaymentId,
        Instant createdAt,
        Instant updatedAt
) {
}
