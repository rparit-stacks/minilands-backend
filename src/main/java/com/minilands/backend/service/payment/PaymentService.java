package com.minilands.backend.service.payment;

import com.minilands.backend.dto.payment.DepositResponse;
import com.minilands.backend.dto.payment.InitiateDepositRequest;
import com.minilands.backend.dto.payment.InitiateDepositResponse;
import com.minilands.backend.dto.payment.ReportDepositRequest;

/**
 * Deposit flow (matches standard Razorpay diagram):
 * <ol>
 *   <li>{@link #initiateDeposit} — order + payment order created on backend</li>
 *   <li>Frontend — client completes Razorpay Checkout</li>
 *   <li>{@link #handleWebhook} — Razorpay webhook → confirm + wallet commit / rollback</li>
 *   <li>{@link #reportDeposit} — optional frontend status sync (same commit/rollback rules)</li>
 * </ol>
 */
public interface PaymentService {

    InitiateDepositResponse initiateDeposit(String userId, InitiateDepositRequest request);

    void handleWebhook(String rawBody, String razorpaySignatureHeader);

    DepositResponse reportDeposit(String userId, ReportDepositRequest request);
}
