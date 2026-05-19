package com.minilands.backend.service.wallet;

import com.minilands.backend.entity.Transaction;
import com.minilands.backend.entity.Wallet;
import com.minilands.backend.entity.enums.TransactionStatus;
import com.minilands.backend.entity.enums.TransactionType;
import com.minilands.backend.repository.TransactionRepository;
import com.minilands.backend.repository.WalletRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class WalletLedgerService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletLedgerService(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    public Wallet requireWallet(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
    }

    public void debit(
            Wallet wallet,
            BigDecimal amount,
            TransactionType type,
            String description,
            String referenceId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        BigDecimal balance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient wallet balance. Available: " + balance + ", required: " + amount);
        }
        wallet.setBalance(balance.subtract(amount));
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);
        saveTransaction(wallet, type, amount, description, referenceId);
    }

    public void credit(
            Wallet wallet,
            BigDecimal amount,
            TransactionType type,
            String description,
            String referenceId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        BigDecimal balance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        wallet.setBalance(balance.add(amount));
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);
        saveTransaction(wallet, type, amount, description, referenceId);
    }

    private void saveTransaction(
            Wallet wallet,
            TransactionType type,
            BigDecimal amount,
            String description,
            String referenceId) {
        Transaction transaction = new Transaction();
        transaction.setWalletId(wallet.getId());
        transaction.setUserId(wallet.getUserId());
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setDescription(description);
        transaction.setReferenceId(referenceId);
        transaction.setCreatedAt(Instant.now());
        transactionRepository.save(transaction);
    }
}
