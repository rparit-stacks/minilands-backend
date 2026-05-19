package com.minilands.backend.service.dashboard.impl;

import com.minilands.backend.dto.dashboard.DashboardResponse;
import com.minilands.backend.dto.investment.HoldingDetailResponse;
import com.minilands.backend.repository.TransactionRepository;
import com.minilands.backend.repository.WalletRepository;
import com.minilands.backend.service.dashboard.DashboardService;
import com.minilands.backend.service.investment.PropertyInvestmentService;
import com.minilands.backend.dto.wallet.TransactionResponse;
import com.minilands.backend.entity.Transaction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final PropertyInvestmentService propertyInvestmentService;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public DashboardServiceImpl(
            PropertyInvestmentService propertyInvestmentService,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository) {
        this.propertyInvestmentService = propertyInvestmentService;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public DashboardResponse getSummary(String userId) {
        List<HoldingDetailResponse> holdings = propertyInvestmentService.getHoldings(userId);

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        BigDecimal totalRoiEarned = BigDecimal.ZERO;

        for (HoldingDetailResponse h : holdings) {
            totalInvested = totalInvested.add(h.costBasis() != null ? h.costBasis() : BigDecimal.ZERO);
            totalCurrentValue = totalCurrentValue.add(
                    h.currentInvestmentValue() != null ? h.currentInvestmentValue() : BigDecimal.ZERO);
            totalRoiEarned = totalRoiEarned.add(h.rentalEarnings() != null ? h.rentalEarnings() : BigDecimal.ZERO);
        }

        BigDecimal roiPercentage = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            roiPercentage = totalCurrentValue.subtract(totalInvested)
                    .divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        BigDecimal walletBalance = walletRepository.findByUserId(userId)
                .map(w -> w.getBalance() != null ? w.getBalance() : BigDecimal.ZERO)
                .orElse(BigDecimal.ZERO);

        var portfolioSummary = new DashboardResponse.PortfolioSummary(
                totalInvested,
                totalCurrentValue,
                totalRoiEarned,
                roiPercentage,
                holdings.size());

        List<TransactionResponse> recent = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(10)
                .map(this::toTransactionResponse)
                .toList();

        return new DashboardResponse(
                portfolioSummary,
                walletBalance,
                holdings,
                Collections.emptyList(),
                Collections.emptyList(),
                recent);
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getCreatedAt());
    }
}
