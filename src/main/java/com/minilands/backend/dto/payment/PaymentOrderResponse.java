package com.minilands.backend.dto.payment;

import java.math.BigDecimal;

public record PaymentOrderResponse(
        String depositId,
        String razorpayOrderId,
        BigDecimal amount,
        String currency
) {
}
