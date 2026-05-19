package com.minilands.backend.service.payment.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.minilands.backend.config.RazorpayProperties;
import com.minilands.backend.config.WalletProperties;
import com.minilands.backend.dto.payment.DepositResponse;
import com.minilands.backend.dto.payment.InitiateDepositRequest;
import com.minilands.backend.dto.payment.InitiateDepositResponse;
import com.minilands.backend.dto.payment.ReportDepositRequest;
import com.minilands.backend.entity.Deposit;
import com.minilands.backend.entity.Transaction;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.Wallet;
import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.entity.enums.DepositStatus;
import com.minilands.backend.entity.enums.NotificationType;
import com.minilands.backend.entity.enums.PaymentReportStatus;
import com.minilands.backend.entity.enums.TransactionStatus;
import com.minilands.backend.entity.enums.TransactionType;
import com.minilands.backend.repository.DepositRepository;
import com.minilands.backend.repository.TransactionRepository;
import com.minilands.backend.repository.UserRepository;
import com.minilands.backend.repository.WalletRepository;
import com.minilands.backend.service.kyc.KycGuard;
import com.minilands.backend.service.notification.NotificationService;
import com.minilands.backend.service.payment.PaymentService;
import com.minilands.backend.service.payment.RazorpayOrderClient;
import com.minilands.backend.service.payment.RazorpaySignatureVerifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final DepositRepository depositRepository;
    private final TransactionRepository transactionRepository;
    private final RazorpayOrderClient razorpayOrderClient;
    private final RazorpaySignatureVerifier signatureVerifier;
    private final NotificationService notificationService;
    private final WalletProperties walletProperties;
    private final RazorpayProperties razorpayProperties;
    private final ObjectMapper objectMapper;

    public PaymentServiceImpl(
            UserRepository userRepository,
            WalletRepository walletRepository,
            DepositRepository depositRepository,
            TransactionRepository transactionRepository,
            RazorpayOrderClient razorpayOrderClient,
            RazorpaySignatureVerifier signatureVerifier,
            NotificationService notificationService,
            WalletProperties walletProperties,
            RazorpayProperties razorpayProperties,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.depositRepository = depositRepository;
        this.transactionRepository = transactionRepository;
        this.razorpayOrderClient = razorpayOrderClient;
        this.signatureVerifier = signatureVerifier;
        this.notificationService = notificationService;
        this.walletProperties = walletProperties;
        this.razorpayProperties = razorpayProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public InitiateDepositResponse initiateDeposit(String userId, InitiateDepositRequest request) {
        User user = requireActiveUserWithKyc(userId);
        walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        validateDepositAmount(request.amount());
        long amountInPaise = toPaise(request.amount());

        Instant now = Instant.now();
        Deposit deposit = new Deposit();
        deposit.setUserId(userId);
        deposit.setAmount(request.amount());
        deposit.setStatus(DepositStatus.CREATED);
        deposit.setCreatedAt(now);
        deposit.setUpdatedAt(now);
        depositRepository.save(deposit);

        RazorpayOrderClient.RazorpayOrder order = razorpayOrderClient.createOrder(
                amountInPaise,
                "deposit_" + deposit.getId(),
                userId);

        deposit.setRazorpayOrderId(order.id());
        deposit.setUpdatedAt(Instant.now());
        depositRepository.save(deposit);

        return new InitiateDepositResponse(
                deposit.getId(),
                deposit.getStatus(),
                razorpayProperties.getKeyId(),
                order.id(),
                request.amount(),
                amountInPaise,
                order.currency() != null ? order.currency() : "INR",
                razorpayProperties.getCompanyName(),
                user.getName(),
                user.getEmail());
    }

    @Override
    @Transactional
    public void handleWebhook(String rawBody, String razorpaySignatureHeader) {
        if (!signatureVerifier.isWebhookSignatureValid(rawBody, razorpaySignatureHeader)) {
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String event = root.path("event").asText();
            JsonNode paymentEntity = root.path("payload").path("payment").path("entity");

            if (paymentEntity.isMissingNode()) {
                return;
            }

            String orderId = paymentEntity.path("order_id").asText(null);
            String paymentId = paymentEntity.path("id").asText(null);
            if (!StringUtils.hasText(orderId)) {
                return;
            }

            Deposit deposit = depositRepository.findByRazorpayOrderId(orderId)
                    .orElse(null);
            if (deposit == null) {
                return;
            }

            if (event.equals("payment.captured") || event.equals("order.paid")) {
                commitDeposit(deposit, paymentId, null);
            } else if (event.equals("payment.failed")) {
                rollbackDeposit(deposit, "Payment failed");
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to process Razorpay webhook", ex);
        }
    }

    @Override
    @Transactional
    public DepositResponse reportDeposit(String userId, ReportDepositRequest request) {
        requireActiveUserWithKyc(userId);
        validateDepositAmount(request.amount());
        validateReportPayload(request);

        Deposit deposit = depositRepository.findByRazorpayOrderId(request.razorpayOrderId())
                .filter(d -> d.getUserId().equals(userId))
                .orElseGet(() -> createDepositRecord(userId, request));

        if (deposit.getStatus() == DepositStatus.PAID) {
            return toDepositResponse(deposit, currentBalance(userId));
        }

        if (request.status() == PaymentReportStatus.SUCCESS) {
            if (!signatureVerifier.isPaymentSignatureValid(
                    request.razorpayOrderId(),
                    request.razorpayPaymentId(),
                    request.razorpaySignature())) {
                rollbackDeposit(deposit, "Invalid payment signature");
                throw new IllegalArgumentException("Invalid payment signature");
            }
            return commitDeposit(deposit, request.razorpayPaymentId(), request.razorpaySignature());
        }

        rollbackDeposit(deposit, request.failureReason());
        return toDepositResponse(deposit, currentBalance(userId));
    }

    private Deposit createDepositRecord(String userId, ReportDepositRequest request) {
        Instant now = Instant.now();
        Deposit deposit = new Deposit();
        deposit.setUserId(userId);
        deposit.setAmount(request.amount());
        deposit.setRazorpayOrderId(request.razorpayOrderId());
        deposit.setStatus(DepositStatus.CREATED);
        deposit.setCreatedAt(now);
        deposit.setUpdatedAt(now);
        return depositRepository.save(deposit);
    }

    private DepositResponse commitDeposit(Deposit deposit, String paymentId, String signature) {
        if (deposit.getStatus() == DepositStatus.PAID) {
            return toDepositResponse(deposit, currentBalance(deposit.getUserId()));
        }

        if (StringUtils.hasText(paymentId)) {
            depositRepository.findByRazorpayPaymentId(paymentId)
                    .filter(existing -> !existing.getId().equals(deposit.getId()))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Payment already processed");
                    });
        }

        Wallet wallet = walletRepository.findByUserId(deposit.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        Instant now = Instant.now();
        BigDecimal amount = deposit.getAmount();

        if (StringUtils.hasText(paymentId)) {
            deposit.setRazorpayPaymentId(paymentId);
        }
        if (StringUtils.hasText(signature)) {
            deposit.setRazorpaySignature(signature);
        }
        deposit.setStatus(DepositStatus.PAID);
        deposit.setUpdatedAt(now);
        depositRepository.save(deposit);

        BigDecimal currentBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        wallet.setBalance(currentBalance.add(amount));
        wallet.setUpdatedAt(now);
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setWalletId(wallet.getId());
        transaction.setUserId(deposit.getUserId());
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setReferenceId(deposit.getRazorpayPaymentId());
        transaction.setDescription("Deposit via Razorpay");
        transaction.setCreatedAt(now);
        transactionRepository.save(transaction);

        notificationService.send(
                deposit.getUserId(),
                NotificationType.DEPOSIT,
                "Deposit successful",
                "₹" + amount + " has been added to your wallet.");

        return toDepositResponse(deposit, wallet.getBalance());
    }

    private void rollbackDeposit(Deposit deposit, String reason) {
        if (deposit.getStatus() == DepositStatus.PAID) {
            return;
        }
        deposit.setStatus(DepositStatus.FAILED);
        deposit.setUpdatedAt(Instant.now());
        depositRepository.save(deposit);

        String message = StringUtils.hasText(reason) ? reason : "Payment failed";
        notificationService.send(
                deposit.getUserId(),
                NotificationType.DEPOSIT,
                "Deposit failed",
                "Your deposit could not be completed. " + message);
    }

    private BigDecimal currentBalance(String userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        return wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
    }

    private User requireActiveUserWithKyc(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active");
        }
        KycGuard.requireApproved(user);
        return user;
    }

    private void validateDepositAmount(BigDecimal amount) {
        if (amount.compareTo(walletProperties.getMinDepositAmount()) < 0) {
            throw new IllegalArgumentException("Minimum deposit is ₹" + walletProperties.getMinDepositAmount());
        }
        if (amount.compareTo(walletProperties.getMaxDepositAmount()) > 0) {
            throw new IllegalArgumentException("Maximum deposit is ₹" + walletProperties.getMaxDepositAmount());
        }
    }

    private void validateReportPayload(ReportDepositRequest request) {
        if (request.status() == PaymentReportStatus.SUCCESS) {
            if (!StringUtils.hasText(request.razorpayPaymentId()) || !StringUtils.hasText(request.razorpaySignature())) {
                throw new IllegalArgumentException("Payment id and signature are required when status is SUCCESS");
            }
        }
    }

    private long toPaise(BigDecimal amountInRupees) {
        return amountInRupees.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private DepositResponse toDepositResponse(Deposit deposit, BigDecimal walletBalance) {
        return new DepositResponse(
                deposit.getId(),
                deposit.getStatus(),
                deposit.getAmount(),
                deposit.getRazorpayOrderId(),
                deposit.getRazorpayPaymentId(),
                walletBalance,
                deposit.getCreatedAt());
    }
}
