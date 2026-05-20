package com.minilands.backend.service.payment.impl;

import com.minilands.backend.dto.payment.AdminDepositResponse;
import com.minilands.backend.entity.Deposit;
import com.minilands.backend.entity.Wallet;
import com.minilands.backend.entity.enums.DepositStatus;
import com.minilands.backend.entity.enums.NotificationType;
import com.minilands.backend.entity.enums.TransactionType;
import com.minilands.backend.repository.DepositRepository;
import com.minilands.backend.service.notification.NotificationService;
import com.minilands.backend.service.payment.AdminDepositService;
import com.minilands.backend.service.wallet.WalletLedgerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminDepositServiceImpl implements AdminDepositService {

    private final DepositRepository depositRepository;
    private final WalletLedgerService walletLedgerService;
    private final NotificationService notificationService;

    public AdminDepositServiceImpl(
            DepositRepository depositRepository,
            WalletLedgerService walletLedgerService,
            NotificationService notificationService) {
        this.depositRepository = depositRepository;
        this.walletLedgerService = walletLedgerService;
        this.notificationService = notificationService;
    }

    @Override
    public List<AdminDepositResponse> listDeposits(DepositStatus status) {
        List<Deposit> deposits = status != null
                ? depositRepository.findByStatusOrderByCreatedAtDesc(status)
                : depositRepository.findAll().stream()
                        .sorted((a, b) -> {
                            if (a.getCreatedAt() == null) return 1;
                            if (b.getCreatedAt() == null) return -1;
                            return b.getCreatedAt().compareTo(a.getCreatedAt());
                        })
                        .collect(Collectors.toList());
        return deposits.stream().map(this::toResponse).toList();
    }

    @Override
    public AdminDepositResponse getById(String depositId) {
        return toResponse(findDeposit(depositId));
    }

    @Override
    @Transactional
    public AdminDepositResponse manuallyComplete(String depositId) {
        Deposit deposit = findDeposit(depositId);
        if (deposit.getStatus() == DepositStatus.PAID) {
            throw new IllegalStateException("Deposit is already completed");
        }
        if (deposit.getStatus() == DepositStatus.FAILED) {
            throw new IllegalArgumentException("Cannot complete a failed deposit");
        }

        Wallet wallet = walletLedgerService.requireWallet(deposit.getUserId());
        walletLedgerService.credit(
                wallet,
                deposit.getAmount(),
                TransactionType.DEPOSIT,
                "Manual deposit completion by admin",
                deposit.getId());

        Instant now = Instant.now();
        deposit.setStatus(DepositStatus.PAID);
        deposit.setUpdatedAt(now);
        Deposit saved = depositRepository.save(deposit);

        notificationService.send(
                deposit.getUserId(),
                NotificationType.DEPOSIT,
                "Deposit completed",
                "₹" + deposit.getAmount() + " has been added to your wallet.");

        return toResponse(saved);
    }

    private Deposit findDeposit(String depositId) {
        return depositRepository.findById(depositId)
                .orElseThrow(() -> new IllegalArgumentException("Deposit not found"));
    }

    private AdminDepositResponse toResponse(Deposit deposit) {
        return new AdminDepositResponse(
                deposit.getId(),
                deposit.getUserId(),
                deposit.getAmount(),
                deposit.getStatus(),
                deposit.getRazorpayOrderId(),
                deposit.getRazorpayPaymentId(),
                deposit.getCreatedAt(),
                deposit.getUpdatedAt());
    }
}
