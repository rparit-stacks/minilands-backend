package com.minilands.backend.service.wallet.impl;

import com.minilands.backend.dto.wallet.BankAccountResponse;
import com.minilands.backend.dto.wallet.TransactionResponse;
import com.minilands.backend.dto.wallet.WithdrawalResponse;
import com.minilands.backend.entity.BankAccount;
import com.minilands.backend.entity.Transaction;
import com.minilands.backend.entity.Wallet;
import com.minilands.backend.entity.Withdrawal;
import com.minilands.backend.entity.enums.WithdrawalStatus;
import com.minilands.backend.repository.WalletRepository;
import com.minilands.backend.repository.WithdrawalRepository;

import java.math.BigDecimal;
import java.util.List;

final class WalletSupport {

    private WalletSupport() {
    }

    static Wallet requireWallet(WalletRepository walletRepository, String userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
    }

    static BigDecimal sumPendingWithdrawals(WithdrawalRepository withdrawalRepository, String userId) {
        return withdrawalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(w -> w.getStatus() == WithdrawalStatus.PENDING)
                .map(Withdrawal::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getCreatedAt());
    }

    static WithdrawalResponse toWithdrawalResponse(Withdrawal withdrawal) {
        return new WithdrawalResponse(
                withdrawal.getId(),
                withdrawal.getUserId(),
                withdrawal.getAmount(),
                withdrawal.getStatus(),
                withdrawal.getBankAccountId(),
                withdrawal.getAdminNote(),
                withdrawal.getProcessedAt(),
                withdrawal.getCreatedAt());
    }

    static BankAccountResponse toBankAccountResponse(BankAccount account) {
        return new BankAccountResponse(
                account.getId(),
                account.getAccountHolderName(),
                maskAccountNumber(account.getAccountNumber()),
                account.getIfscCode(),
                account.getBankName(),
                account.isPrimary(),
                account.isVerified());
    }

    static void clearPrimaryFlags(List<BankAccount> accounts) {
        accounts.stream().filter(BankAccount::isPrimary).forEach(account -> account.setPrimary(false));
    }

    private static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
