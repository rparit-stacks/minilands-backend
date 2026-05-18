package com.minilands.backend.service.wallet;

import com.minilands.backend.dto.payment.PaymentOrderResponse;
import com.minilands.backend.dto.wallet.DepositRequest;
import com.minilands.backend.dto.wallet.TransactionResponse;
import com.minilands.backend.dto.wallet.WalletBalanceResponse;
import com.minilands.backend.dto.wallet.WithdrawalRequest;

import java.util.List;

/**
 * Investor wallet operations: balance, deposit initiation, withdrawal request, history (SRP).
 */
public interface WalletService {

    WalletBalanceResponse getBalance(String userId);

    PaymentOrderResponse initiateDeposit(String userId, DepositRequest request);

    String requestWithdrawal(String userId, WithdrawalRequest request);

    List<TransactionResponse> getTransactionHistory(String userId);
}
