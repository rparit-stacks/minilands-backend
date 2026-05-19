package com.minilands.backend.service.wallet;

import com.minilands.backend.dto.payment.DepositResponse;
import com.minilands.backend.dto.payment.InitiateDepositRequest;
import com.minilands.backend.dto.payment.InitiateDepositResponse;
import com.minilands.backend.dto.payment.ReportDepositRequest;
import com.minilands.backend.dto.wallet.TransactionResponse;
import com.minilands.backend.dto.wallet.WalletBalanceResponse;
import com.minilands.backend.dto.wallet.WithdrawalRequest;
import com.minilands.backend.dto.wallet.WithdrawalResponse;

import java.util.List;

/**
 * Investor wallet operations: balance, deposit reporting, withdrawal request, history (SRP).
 * <p>
 * Deposits: {@link #initiateDeposit} → frontend Checkout → webhook / {@link #reportDeposit}.
 */
public interface WalletService {

    WalletBalanceResponse getBalance(String userId);

    InitiateDepositResponse initiateDeposit(String userId, InitiateDepositRequest request);

    DepositResponse reportDeposit(String userId, ReportDepositRequest request);

    WithdrawalResponse requestWithdrawal(String userId, WithdrawalRequest request);

    List<WithdrawalResponse> getWithdrawals(String userId);

    List<TransactionResponse> getTransactionHistory(String userId);
}
