package com.minilands.backend.service.wallet.impl;

import com.minilands.backend.dto.wallet.AdminActionRequest;
import com.minilands.backend.dto.wallet.WithdrawalResponse;
import com.minilands.backend.entity.Transaction;
import com.minilands.backend.entity.Wallet;
import com.minilands.backend.entity.Withdrawal;
import com.minilands.backend.entity.enums.NotificationType;
import com.minilands.backend.entity.enums.TransactionStatus;
import com.minilands.backend.entity.enums.TransactionType;
import com.minilands.backend.entity.enums.WithdrawalStatus;
import com.minilands.backend.repository.TransactionRepository;
import com.minilands.backend.repository.WalletRepository;
import com.minilands.backend.repository.WithdrawalRepository;
import com.minilands.backend.service.notification.NotificationService;
import com.minilands.backend.service.wallet.AdminWithdrawalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminWithdrawalServiceImpl implements AdminWithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    public AdminWithdrawalServiceImpl(
            WithdrawalRepository withdrawalRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            NotificationService notificationService) {
        this.withdrawalRepository = withdrawalRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
    }

    @Override
    public List<WithdrawalResponse> listWithdrawals(WithdrawalStatus status) {
        List<Withdrawal> results = status != null
                ? withdrawalRepository.findByStatus(status)
                : withdrawalRepository.findAll().stream()
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .collect(Collectors.toList());
        return results.stream().map(WalletSupport::toWithdrawalResponse).toList();
    }

    @Override
    public WithdrawalResponse getById(String withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal not found"));
        return WalletSupport.toWithdrawalResponse(withdrawal);
    }

    @Override
    @Transactional
    public void approve(String adminId, String withdrawalId, AdminActionRequest request) {
        Withdrawal withdrawal = requirePending(withdrawalId);
        Wallet wallet = WalletSupport.requireWallet(walletRepository, withdrawal.getUserId());

        java.math.BigDecimal balance = wallet.getBalance() != null ? wallet.getBalance() : java.math.BigDecimal.ZERO;
        if (balance.compareTo(withdrawal.getAmount()) < 0) {
            throw new IllegalArgumentException("User has insufficient wallet balance for this withdrawal");
        }

        Instant now = Instant.now();
        wallet.setBalance(balance.subtract(withdrawal.getAmount()));
        wallet.setUpdatedAt(now);
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setWalletId(wallet.getId());
        transaction.setUserId(withdrawal.getUserId());
        transaction.setType(TransactionType.WITHDRAWAL);
        transaction.setAmount(withdrawal.getAmount());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setReferenceId(withdrawal.getId());
        transaction.setDescription("Withdrawal to bank account");
        transaction.setCreatedAt(now);
        transactionRepository.save(transaction);

        withdrawal.setStatus(WithdrawalStatus.COMPLETED);
        withdrawal.setApprovedByAdminId(adminId);
        withdrawal.setAdminNote(request != null ? request.note() : null);
        withdrawal.setTransactionId(transaction.getId());
        withdrawal.setProcessedAt(now);
        withdrawal.setUpdatedAt(now);
        withdrawalRepository.save(withdrawal);

        notificationService.send(
                withdrawal.getUserId(),
                NotificationType.WITHDRAWAL,
                "Withdrawal approved",
                "₹" + withdrawal.getAmount() + " will be transferred to your bank account within 1–3 business days.");
    }

    @Override
    @Transactional
    public void reject(String adminId, String withdrawalId, AdminActionRequest request) {
        Withdrawal withdrawal = requirePending(withdrawalId);
        Instant now = Instant.now();

        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawal.setRejectedByAdminId(adminId);
        withdrawal.setAdminNote(request != null ? request.note() : null);
        withdrawal.setProcessedAt(now);
        withdrawal.setUpdatedAt(now);
        withdrawalRepository.save(withdrawal);

        String reason = request != null && request.note() != null ? request.note() : "No reason provided";
        notificationService.send(
                withdrawal.getUserId(),
                NotificationType.WITHDRAWAL,
                "Withdrawal rejected",
                "Your withdrawal request was rejected. Reason: " + reason);
    }

    private Withdrawal requirePending(String withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal not found"));
        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new IllegalArgumentException("Only pending withdrawals can be processed");
        }
        return withdrawal;
    }
}
