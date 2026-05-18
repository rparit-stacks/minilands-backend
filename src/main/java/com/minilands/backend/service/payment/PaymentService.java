package com.minilands.backend.service.payment;

import com.minilands.backend.dto.payment.PaymentOrderResponse;
import com.minilands.backend.dto.payment.PaymentWebhookRequest;

import java.math.BigDecimal;

/**
 * Razorpay integration: order creation and webhook handling (SRP).
 * Decoupled from {@link com.minilands.backend.service.wallet.WalletService} (DIP).
 */
public interface PaymentService {

    PaymentOrderResponse createOrder(String userId, BigDecimal amount);

    void handleWebhook(PaymentWebhookRequest request);
}
