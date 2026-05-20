package com.minilands.backend.service.dashboard.impl;

import com.minilands.backend.dto.dashboard.DashboardResponse;
import com.minilands.backend.dto.investment.HoldingDetailResponse;
import com.minilands.backend.dto.voting.ProposalResponse;
import com.minilands.backend.dto.wallet.TransactionResponse;
import com.minilands.backend.entity.Transaction;
import com.minilands.backend.entity.enums.HoldingStatus;
import com.minilands.backend.entity.enums.ProposalStatus;
import com.minilands.backend.repository.PropertyHoldingRepository;
import com.minilands.backend.repository.PropertySaleProposalRepository;
import com.minilands.backend.repository.RoiEarningRepository;
import com.minilands.backend.repository.TransactionRepository;
import com.minilands.backend.repository.WalletRepository;
import com.minilands.backend.service.dashboard.DashboardService;
import com.minilands.backend.service.investment.PropertyInvestmentService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final int ROI_TIMELINE_MONTHS = 6;
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy");

    // Fixed palette — frontend can override if needed
    private static final String[] CHART_COLORS = {
            "#6366F1", "#10B981", "#F59E0B", "#EF4444", "#3B82F6",
            "#8B5CF6", "#14B8A6", "#F97316", "#EC4899", "#84CC16"
    };

    private final PropertyInvestmentService propertyInvestmentService;
    private final PropertyHoldingRepository holdingRepository;
    private final PropertySaleProposalRepository proposalRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final RoiEarningRepository roiEarningRepository;

    public DashboardServiceImpl(
            PropertyInvestmentService propertyInvestmentService,
            PropertyHoldingRepository holdingRepository,
            PropertySaleProposalRepository proposalRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            RoiEarningRepository roiEarningRepository) {
        this.propertyInvestmentService = propertyInvestmentService;
        this.holdingRepository = holdingRepository;
        this.proposalRepository = proposalRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.roiEarningRepository = roiEarningRepository;
    }

    @Override
    public DashboardResponse getSummary(String userId) {

        // ── RAW DATA ─────────────────────────────────────────────────────────────
        List<HoldingDetailResponse> holdings = propertyInvestmentService.getHoldings(userId);

        BigDecimal walletBalance = walletRepository.findByUserId(userId)
                .map(w -> w.getBalance() != null ? w.getBalance() : BigDecimal.ZERO)
                .orElse(BigDecimal.ZERO);

        // ── PORTFOLIO AGGREGATION ────────────────────────────────────────────────
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        BigDecimal totalRoiEarned = BigDecimal.ZERO;

        for (HoldingDetailResponse h : holdings) {
            totalInvested = totalInvested.add(safe(h.costBasis()));
            totalCurrentValue = totalCurrentValue.add(safe(h.currentInvestmentValue()));
            totalRoiEarned = totalRoiEarned.add(safe(h.rentalEarnings()));
        }

        BigDecimal unrealizedPnl = totalCurrentValue.subtract(totalInvested);
        BigDecimal roiPct = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            roiPct = unrealizedPnl
                    .divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // ── TEXT SUMMARY ─────────────────────────────────────────────────────────
        DashboardResponse.Summary summary = new DashboardResponse.Summary(
                totalInvested,
                totalCurrentValue,
                totalRoiEarned,
                unrealizedPnl,
                roiPct,
                holdings.size(),
                formatRupee(totalInvested),
                formatRupee(totalCurrentValue),
                formatRupee(totalRoiEarned),
                formatPnl(unrealizedPnl, roiPct),
                roiPct.toPlainString() + "%",
                formatRupee(walletBalance)
        );

        // ── PENDING VOTES ─────────────────────────────────────────────────────────
        List<ProposalResponse> pendingVotes = holdingRepository
                .findByUserIdAndStatus(userId, HoldingStatus.ACTIVE)
                .stream()
                .flatMap(h -> proposalRepository.findByPropertyId(h.getPropertyId()).stream())
                .filter(p -> p.getStatus() == ProposalStatus.ACTIVE
                        || p.getStatus() == ProposalStatus.PENDING_ADMIN_APPROVAL)
                .map(p -> new ProposalResponse(
                        p.getId(), p.getPropertyId(), p.getStatus(),
                        p.getThresholdReachedAt(),
                        p.getReviewedByAdminId(), p.getAdminNote(), p.getReviewedAt()))
                .distinct()
                .toList();

        // ── RECENT TRANSACTIONS ───────────────────────────────────────────────────
        List<TransactionResponse> recentTransactions = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(10)
                .map(this::toTransactionResponse)
                .toList();

        // ── CHART 1: Portfolio Allocation (pie — value per property) ─────────────
        List<DashboardResponse.PieSlice> portfolioAllocation =
                buildPortfolioAllocationPie(holdings, totalCurrentValue);

        // ── CHART 2: Portfolio Composition (pie — principal vs ROI vs wallet) ────
        List<DashboardResponse.PieSlice> portfolioComposition =
                buildPortfolioCompositionPie(totalInvested, totalRoiEarned, walletBalance);

        // ── CHART 3: Monthly ROI Timeline (line/bar — last 6 months) ─────────────
        List<DashboardResponse.MonthlyDataPoint> roiTimeline =
                buildRoiTimeline(userId);

        DashboardResponse.Charts charts = new DashboardResponse.Charts(
                portfolioAllocation,
                portfolioComposition,
                roiTimeline);

        return new DashboardResponse(
                summary,
                walletBalance,
                holdings,
                pendingVotes,
                recentTransactions,
                charts);
    }

    // ── CHART BUILDERS ────────────────────────────────────────────────────────────

    private List<DashboardResponse.PieSlice> buildPortfolioAllocationPie(
            List<HoldingDetailResponse> holdings, BigDecimal totalCurrentValue) {

        if (holdings.isEmpty() || totalCurrentValue.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        List<DashboardResponse.PieSlice> slices = new ArrayList<>();
        for (int i = 0; i < holdings.size(); i++) {
            HoldingDetailResponse h = holdings.get(i);
            BigDecimal value = safe(h.currentInvestmentValue());
            BigDecimal pct = value
                    .divide(totalCurrentValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            slices.add(new DashboardResponse.PieSlice(
                    h.propertyName(),
                    value,
                    pct,
                    CHART_COLORS[i % CHART_COLORS.length]));
        }
        return slices;
    }

    private List<DashboardResponse.PieSlice> buildPortfolioCompositionPie(
            BigDecimal totalInvested, BigDecimal totalRoiEarned, BigDecimal walletBalance) {

        BigDecimal total = totalInvested.add(totalRoiEarned).add(walletBalance);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        List<DashboardResponse.PieSlice> slices = new ArrayList<>();

        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            slices.add(new DashboardResponse.PieSlice(
                    "Invested Principal",
                    totalInvested,
                    pct(totalInvested, total),
                    "#6366F1"));
        }
        if (totalRoiEarned.compareTo(BigDecimal.ZERO) > 0) {
            slices.add(new DashboardResponse.PieSlice(
                    "ROI Earned",
                    totalRoiEarned,
                    pct(totalRoiEarned, total),
                    "#10B981"));
        }
        if (walletBalance.compareTo(BigDecimal.ZERO) > 0) {
            slices.add(new DashboardResponse.PieSlice(
                    "Wallet Balance",
                    walletBalance,
                    pct(walletBalance, total),
                    "#F59E0B"));
        }
        return slices;
    }

    private List<DashboardResponse.MonthlyDataPoint> buildRoiTimeline(String userId) {
        // Build ordered map of last N months with zero defaults
        Map<YearMonth, BigDecimal> monthlyRoi = new LinkedHashMap<>();
        YearMonth current = YearMonth.now(ZoneOffset.UTC);
        for (int i = ROI_TIMELINE_MONTHS - 1; i >= 0; i--) {
            monthlyRoi.put(current.minusMonths(i), BigDecimal.ZERO);
        }

        YearMonth oldest = current.minusMonths(ROI_TIMELINE_MONTHS - 1);
        Instant cutoff = oldest.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // Sum ROI earnings per calendar month
        roiEarningRepository.findByUserIdOrderByEarnedOnDateDesc(userId)
                .stream()
                .filter(e -> e.getEarnedOnDate() != null && !e.getEarnedOnDate().isBefore(cutoff))
                .forEach(e -> {
                    YearMonth ym = YearMonth.from(e.getEarnedOnDate().atZone(ZoneOffset.UTC));
                    monthlyRoi.computeIfPresent(ym, (k, v) -> v.add(safe(e.getAmount())));
                });

        return monthlyRoi.entrySet().stream()
                .map(entry -> new DashboardResponse.MonthlyDataPoint(
                        entry.getKey().format(MONTH_LABEL),
                        entry.getValue().setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────────

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal pct(BigDecimal part, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return part.divide(total, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String formatRupee(BigDecimal amount) {
        if (amount == null) return "₹0";
        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"));
        fmt.setMinimumFractionDigits(0);
        fmt.setMaximumFractionDigits(0);
        return "₹" + fmt.format(amount.setScale(0, RoundingMode.HALF_UP));
    }

    private String formatPnl(BigDecimal pnl, BigDecimal pct) {
        String sign = pnl.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + formatRupee(pnl) + " (" + (pnl.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + pct + "%)";
    }

    private TransactionResponse toTransactionResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getType(),
                t.getAmount(),
                t.getStatus(),
                t.getDescription(),
                t.getCreatedAt());
    }
}
