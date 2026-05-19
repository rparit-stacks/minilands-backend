package com.minilands.backend.dto.payment;

import com.minilands.backend.entity.enums.PaymentReportStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Frontend sends this after Razorpay Checkout ends (success, failure, or cancelled).
 * Razorpay order + checkout run entirely on the client.
 */
public record ReportDepositRequest(
        @NotNull @DecimalMin("1.00") BigDecimal amount,
        @NotBlank String razorpayOrderId,
        String razorpayPaymentId,
        String razorpaySignature,
        @NotNull PaymentReportStatus status,
        String failureReason
) {
}
