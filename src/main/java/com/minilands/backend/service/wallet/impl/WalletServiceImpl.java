package com.minilands.backend.service.wallet.impl;

import com.minilands.backend.config.WalletProperties;
import com.minilands.backend.dto.payment.DepositResponse;
import com.minilands.backend.dto.payment.InitiateDepositRequest;
import com.minilands.backend.dto.payment.InitiateDepositResponse;
import com.minilands.backend.dto.payment.ReportDepositRequest;
import com.minilands.backend.dto.wallet.TransactionMapper;
import com.minilands.backend.dto.wallet.TransactionResponse;
import com.minilands.backend.dto.wallet.WalletBalanceResponse;
import com.minilands.backend.dto.wallet.WithdrawalRequest;
import com.minilands.backend.dto.wallet.WithdrawalResponse;
import com.minilands.backend.entity.BankAccount;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.Wallet;
import com.minilands.backend.entity.Withdrawal;
import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.entity.enums.NotificationType;
import com.minilands.backend.entity.enums.WithdrawalStatus;
import com.minilands.backend.repository.BankAccountRepository;
import com.minilands.backend.repository.TransactionRepository;
import com.minilands.backend.repository.UserRepository;
import com.minilands.backend.repository.WalletRepository;
import com.minilands.backend.repository.WithdrawalRepository;
import com.minilands.backend.service.kyc.KycGuard;
import com.minilands.backend.service.notification.NotificationService;
import com.minilands.backend.service.payment.PaymentService;
import com.minilands.backend.service.wallet.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final WalletProperties walletProperties;

    public WalletServiceImpl(
            UserRepository userRepository,
            WalletRepository walletRepository,
            WithdrawalRepository withdrawalRepository,
            BankAccountRepository bankAccountRepository,
            TransactionRepository transactionRepository,
            PaymentService paymentService,
            NotificationService notificationService,
            WalletProperties walletProperties) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
        this.walletProperties = walletProperties;
    }

    @Override
    public WalletBalanceResponse getBalance(String userId) {
        Wallet wallet = WalletSupport.requireWallet(walletRepository, userId);
        BigDecimal balance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        BigDecimal pending = WalletSupport.sumPendingWithdrawals(withdrawalRepository, userId);
        BigDecimal available = balance.subtract(pending).max(BigDecimal.ZERO);

        return new WalletBalanceResponse(
                wallet.getId(),
                userId,
                balance,
                pending,
                available,
                wallet.getCurrency());
    }

    @Override
    public InitiateDepositResponse initiateDeposit(String userId, InitiateDepositRequest request) {
        return paymentService.initiateDeposit(userId, request);
    }

    @Override
    public DepositResponse reportDeposit(String userId, ReportDepositRequest request) {
        return paymentService.reportDeposit(userId, request);
    }

    @Override
    @Transactional
    public WithdrawalResponse requestWithdrawal(String userId, WithdrawalRequest request) {
        User user = findActiveUser(userId);
        KycGuard.requireApproved(user);
        validateWithdrawalAmount(request.amount());

        Wallet wallet = WalletSupport.requireWallet(walletRepository, userId);
        BigDecimal balance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        BigDecimal pending = WalletSupport.sumPendingWithdrawals(withdrawalRepository, userId);
        BigDecimal available = balance.subtract(pending);

        if (request.amount().compareTo(available) > 0) {
            throw new IllegalArgumentException("Insufficient available balance");
        }

        BankAccount bankAccount = bankAccountRepository.findById(request.bankAccountId())
                .filter(account -> account.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found"));

        if (!bankAccount.isVerified()) {
            throw new IllegalArgumentException("Bank account is not verified yet");
        }

        Instant now = Instant.now();
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setUserId(userId);
        withdrawal.setWalletId(wallet.getId());
        withdrawal.setBankAccountId(bankAccount.getId());
        withdrawal.setAmount(request.amount());
        withdrawal.setStatus(WithdrawalStatus.PENDING);
        withdrawal.setCreatedAt(now);
        withdrawal.setUpdatedAt(now);
        withdrawalRepository.save(withdrawal);

        notificationService.send(
                userId,
                NotificationType.WITHDRAWAL,
                "Withdrawal requested",
                "Your withdrawal of ₹" + request.amount() + " is pending admin approval.");

        return WalletSupport.toWithdrawalResponse(withdrawal);
    }

    @Override
    public List<WithdrawalResponse> getWithdrawals(String userId) {
        WalletSupport.requireWallet(walletRepository, userId);
        return withdrawalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(WalletSupport::toWithdrawalResponse)
                .toList();
    }

    @Override
    public List<TransactionResponse> getTransactionHistory(String userId) {
        WalletSupport.requireWallet(walletRepository, userId);
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(TransactionMapper::toResponse)
                .toList();
    }

    private User findActiveUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active");
        }
        return user;
    }

    private void validateWithdrawalAmount(BigDecimal amount) {
        if (amount.compareTo(walletProperties.getMinWithdrawalAmount()) < 0) {
            throw new IllegalArgumentException("Minimum withdrawal is ₹" + walletProperties.getMinWithdrawalAmount());
        }
        if (amount.compareTo(walletProperties.getMaxWithdrawalAmount()) > 0) {
            throw new IllegalArgumentException("Maximum withdrawal is ₹" + walletProperties.getMaxWithdrawalAmount());
        }
    }
}
