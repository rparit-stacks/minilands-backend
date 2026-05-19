package com.minilands.backend.dto.payment;

import com.minilands.backend.entity.enums.DepositStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record DepositResponse(
        String depositId,
        DepositStatus status,
        BigDecimal amount,
        String razorpayOrderId,
        String razorpayPaymentId,
        BigDecimal walletBalance,
        Instant createdAt
) {
}
