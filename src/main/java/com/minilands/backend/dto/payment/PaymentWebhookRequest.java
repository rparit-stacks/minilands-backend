package com.minilands.backend.dto.payment;

public record PaymentWebhookRequest(
        String razorpayOrderId,
        String razorpayPaymentId,
        String razorpaySignature,
        String event,
        String payload
) {
}
