package com.minilands.backend.dto.payment;

import com.minilands.backend.entity.enums.DepositStatus;

import java.math.BigDecimal;

/**
 * Step 1–2: deposit order created + Razorpay payment order — frontend opens Checkout with these fields.
 */
public record InitiateDepositResponse(
        String depositId,
        DepositStatus status,
        String keyId,
        String razorpayOrderId,
        BigDecimal amount,
        long amountInPaise,
        String currency,
        String companyName,
        String prefillName,
        String prefillEmail
) {
}
