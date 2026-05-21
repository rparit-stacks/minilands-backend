package com.minilands.backend.service.admin.impl;

import com.minilands.backend.dto.admin.AdminWalletAdjustDirection;
import com.minilands.backend.dto.admin.AdminWalletAdjustRequest;
import com.minilands.backend.dto.admin.AdminWalletDetailResponse;
import com.minilands.backend.dto.admin.AdminWalletRowResponse;
import com.minilands.backend.dto.admin.AdminWalletTransactionsPage;
import com.minilands.backend.dto.wallet.TransactionMapper;
import com.minilands.backend.dto.wallet.WalletBalanceResponse;
import com.minilands.backend.dto.wallet.WithdrawalResponse;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.Wallet;
import com.minilands.backend.entity.enums.TransactionType;
import com.minilands.backend.repository.TransactionRepository;
import com.minilands.backend.repository.UserRepository;
import com.minilands.backend.repository.WalletRepository;
import com.minilands.backend.service.admin.AdminWalletService;
import com.minilands.backend.service.wallet.WalletLedgerService;
import com.minilands.backend.service.wallet.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminWalletServiceImpl implements AdminWalletService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int MAX_TX_PAGE_SIZE = 100;
    private static final int MAX_WITHDRAWALS_IN_DETAIL = 50;

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WalletLedgerService walletLedgerService;
    private final WalletService walletService;
    private final TransactionRepository transactionRepository;

    public AdminWalletServiceImpl(
            WalletRepository walletRepository,
            UserRepository userRepository,
            WalletLedgerService walletLedgerService,
            WalletService walletService,
            TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.walletLedgerService = walletLedgerService;
        this.walletService = walletService;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public List<AdminWalletRowResponse> listAll() {
        List<Wallet> wallets = walletRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
        if (wallets.isEmpty()) {
            return List.of();
        }
        List<String> userIds = wallets.stream().map(Wallet::getUserId).distinct().toList();
        Map<String, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<AdminWalletRowResponse> rows = new ArrayList<>(wallets.size());
        for (Wallet w : wallets) {
            User u = users.get(w.getUserId());
            rows.add(toRow(w, u));
        }
        return rows;
    }

    @Override
    public AdminWalletRowResponse adjust(String userId, AdminWalletAdjustRequest request, String actingAdminId) {
        userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Wallet wallet = ensureWallet(userId);

        String ref = "admin-adjust:" + actingAdminId + ":" + Instant.now().toEpochMilli();
        String desc = buildDescription(request);

        if (request.direction() == AdminWalletAdjustDirection.CREDIT) {
            walletLedgerService.credit(wallet, request.amount(), TransactionType.ADMIN_CREDIT, desc, ref);
        } else {
            walletLedgerService.debit(wallet, request.amount(), TransactionType.ADMIN_DEBIT, desc, ref);
        }

        Wallet updated = walletRepository.findByUserId(userId).orElseThrow();
        User user = userRepository.findById(userId).orElse(null);
        return toRow(updated, user);
    }

    @Override
    public AdminWalletDetailResponse getDetail(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
        AdminWalletRowResponse profile = toRow(wallet != null ? wallet : syntheticWallet(userId), user);

        if (wallet == null) {
            WalletBalanceResponse balance = new WalletBalanceResponse(null, userId, ZERO, ZERO, ZERO, "INR");
            return new AdminWalletDetailResponse(profile, balance, List.of());
        }

        WalletBalanceResponse balance = walletService.getBalance(userId);
        List<WithdrawalResponse> withdrawals = walletService.getWithdrawals(userId).stream()
                .limit(MAX_WITHDRAWALS_IN_DETAIL)
                .toList();
        return new AdminWalletDetailResponse(profile, balance, withdrawals);
    }

    @Override
    public AdminWalletTransactionsPage listTransactions(String userId, TransactionType type, int page, int size) {
        userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (walletRepository.findByUserId(userId).isEmpty()) {
            return new AdminWalletTransactionsPage(List.of(), Math.max(0, page), clampSize(size), 0, 0);
        }

        int safePage = Math.max(0, page);
        int safeSize = clampSize(size);
        var pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<com.minilands.backend.entity.Transaction> result = type == null
                ? transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                : transactionRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);

        List<com.minilands.backend.dto.wallet.TransactionResponse> content = result.getContent().stream()
                .map(TransactionMapper::toResponse)
                .toList();

        return new AdminWalletTransactionsPage(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private static int clampSize(int size) {
        if (size < 1) {
            return 20;
        }
        return Math.min(size, MAX_TX_PAGE_SIZE);
    }

    private Wallet ensureWallet(String userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            Wallet w = new Wallet();
            w.setUserId(userId);
            w.setBalance(ZERO);
            w.setCurrency("INR");
            w.setUpdatedAt(Instant.now());
            return walletRepository.save(w);
        });
    }

    private static Wallet syntheticWallet(String userId) {
        Wallet w = new Wallet();
        w.setUserId(userId);
        w.setBalance(ZERO);
        w.setCurrency("INR");
        return w;
    }

    private static String buildDescription(AdminWalletAdjustRequest request) {
        String base = request.direction() == AdminWalletAdjustDirection.CREDIT
                ? "Admin credit"
                : "Admin debit";
        if (request.note() != null && !request.note().isBlank()) {
            return base + ": " + request.note().trim();
        }
        return base;
    }

    private static AdminWalletRowResponse toRow(Wallet w, User u) {
        String email = u != null && u.getEmail() != null ? u.getEmail() : "—";
        String name = u != null && u.getName() != null && !u.getName().isBlank() ? u.getName() : "—";
        BigDecimal bal = w.getBalance() != null ? w.getBalance() : ZERO;
        return new AdminWalletRowResponse(
                w.getId(),
                w.getUserId(),
                email,
                name,
                bal,
                w.getCurrency() != null ? w.getCurrency() : "INR",
                w.getUpdatedAt());
    }
}
