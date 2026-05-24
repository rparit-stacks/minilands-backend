package com.minilands.backend.service.dashboard.impl;

import com.minilands.backend.dto.dashboard.AdminDashboardResponse;
import com.minilands.backend.dto.dashboard.AdminDashboardResponse.ActionQueue;
import com.minilands.backend.dto.dashboard.AdminDashboardResponse.Charts;
import com.minilands.backend.dto.dashboard.AdminDashboardResponse.MonthlyBarPoint;
import com.minilands.backend.dto.dashboard.AdminDashboardResponse.PieSlice;
import com.minilands.backend.dto.dashboard.AdminDashboardResponse.PlatformSummary;
import com.minilands.backend.dto.dashboard.AdminDashboardResponse.SaleVoteItem;
import com.minilands.backend.entity.MonthlyPaymentRun;
import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.entity.enums.ApprovalStatus;
import com.minilands.backend.entity.enums.HoldingStatus;
import com.minilands.backend.entity.enums.KycStatus;
import com.minilands.backend.entity.enums.MarketplaceListingStatus;
import com.minilands.backend.entity.enums.ProposalStatus;
import com.minilands.backend.entity.enums.PropertyStatus;
import com.minilands.backend.entity.enums.WithdrawalStatus;
import com.minilands.backend.repository.KycDocumentRepository;
import com.minilands.backend.repository.MonthlyPaymentRunRepository;
import com.minilands.backend.repository.PropertyHoldingRepository;
import com.minilands.backend.repository.PropertyRepository;
import com.minilands.backend.repository.PropertySaleProposalRepository;
import com.minilands.backend.repository.ShareListingRepository;
import com.minilands.backend.repository.UserRepository;
import com.minilands.backend.repository.WithdrawalRepository;
import com.minilands.backend.service.dashboard.AdminDashboardService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final String[] PIE_COLORS = {
            "#4F46E5", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6",
            "#06B6D4", "#F97316", "#EC4899", "#14B8A6", "#6366F1"
    };

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyHoldingRepository holdingRepository;
    private final MonthlyPaymentRunRepository monthlyPaymentRunRepository;
    private final ShareListingRepository shareListingRepository;
    private final PropertySaleProposalRepository proposalRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final WithdrawalRepository withdrawalRepository;

    public AdminDashboardServiceImpl(
            UserRepository userRepository,
            PropertyRepository propertyRepository,
            PropertyHoldingRepository holdingRepository,
            MonthlyPaymentRunRepository monthlyPaymentRunRepository,
            ShareListingRepository shareListingRepository,
            PropertySaleProposalRepository proposalRepository,
            KycDocumentRepository kycDocumentRepository,
            WithdrawalRepository withdrawalRepository) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.holdingRepository = holdingRepository;
        this.monthlyPaymentRunRepository = monthlyPaymentRunRepository;
        this.shareListingRepository = shareListingRepository;
        this.proposalRepository = proposalRepository;
        this.kycDocumentRepository = kycDocumentRepository;
        this.withdrawalRepository = withdrawalRepository;
    }

    @Override
    public AdminDashboardResponse getDashboard() {
        List<Property> allProperties = propertyRepository.findAll();
        List<User> allUsers = userRepository.findAll();
        List<MonthlyPaymentRun> allMonthlyRuns = monthlyPaymentRunRepository.findAll();

        PlatformSummary summary = buildSummary(allProperties, allUsers, allMonthlyRuns);
        ActionQueue actionQueue = buildActionQueue();
        Charts charts = buildCharts(allProperties, allUsers, allMonthlyRuns);

        return new AdminDashboardResponse(summary, actionQueue, charts);
    }

    // ── Summary ───────────────────────────────────────────────────────────────────

    private PlatformSummary buildSummary(List<Property> properties, List<User> users,
                                          List<MonthlyPaymentRun> monthlyRuns) {
        long totalUsers = users.size();
        long activeUsers = users.stream()
                .filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE)
                .count();

        long totalProperties = properties.size();
        long activeProperties = properties.stream()
                .filter(p -> p.getStatus() == PropertyStatus.ACTIVE)
                .count();
        long soldProperties = properties.stream()
                .filter(p -> p.getStatus() == PropertyStatus.SOLD)
                .count();

        BigDecimal totalFundsRaised = properties.stream()
                .map(p -> p.getTotalRaised() != null ? p.getTotalRaised() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRoiDistributed = monthlyRuns.stream()
                .map(r -> r.getTotalDistributed() != null ? r.getTotalDistributed() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalActiveHoldings = holdingRepository.countByStatus(HoldingStatus.ACTIVE);

        long activeListingsCount = shareListingRepository.findAll().stream()
                .filter(l -> l.getStatus() == MarketplaceListingStatus.ACTIVE)
                .count();

        long activeSaleVotes = properties.stream()
                .filter(p -> p.getSaleVotePercent().compareTo(BigDecimal.ZERO) > 0)
                .count();

        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"));

        return new PlatformSummary(
                totalUsers, activeUsers,
                totalProperties, activeProperties, soldProperties,
                totalFundsRaised, totalRoiDistributed,
                totalActiveHoldings,
                activeListingsCount,
                activeSaleVotes,
                fmt.format(totalFundsRaised),
                fmt.format(totalRoiDistributed));
    }

    // ── Action Queue ──────────────────────────────────────────────────────────────

    private ActionQueue buildActionQueue() {
        long pendingKyc = kycDocumentRepository.findByStatus(ApprovalStatus.PENDING).size();
        long pendingWithdrawals = withdrawalRepository.findByStatus(WithdrawalStatus.PENDING).size();
        long pendingSaleProposals = proposalRepository.findByStatus(ProposalStatus.PENDING_ADMIN_APPROVAL).size();
        return new ActionQueue(pendingKyc, pendingWithdrawals, pendingSaleProposals);
    }

    // ── Charts ────────────────────────────────────────────────────────────────────

    private Charts buildCharts(List<Property> properties, List<User> users,
                                List<MonthlyPaymentRun> monthlyRuns) {
        return new Charts(
                buildPropertiesByStatusPie(properties),
                buildUsersByKycStatusPie(users),
                buildMonthlyFundsRaised(properties),
                buildMonthlyRoiPaid(monthlyRuns),
                buildMonthlyNewUsers(users),
                buildActiveSaleVoteDetails(properties));
    }

    private List<PieSlice> buildPropertiesByStatusPie(List<Property> properties) {
        Map<PropertyStatus, Long> counts = new LinkedHashMap<>();
        for (PropertyStatus s : PropertyStatus.values()) {
            counts.put(s, 0L);
        }
        for (Property p : properties) {
            counts.merge(p.getStatus(), 1L, (a, b) -> a + b);
        }

        List<PieSlice> slices = new ArrayList<>();
        long total = properties.size();
        int colorIdx = 0;
        for (Map.Entry<PropertyStatus, Long> entry : counts.entrySet()) {
            if (entry.getValue() == 0) continue;
            BigDecimal pct = total > 0
                    ? BigDecimal.valueOf(entry.getValue())
                            .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            slices.add(new PieSlice(
                    entry.getKey().name(),
                    entry.getValue(),
                    pct,
                    PIE_COLORS[colorIdx++ % PIE_COLORS.length]));
        }
        return slices;
    }

    private List<PieSlice> buildUsersByKycStatusPie(List<User> users) {
        long approved = users.stream().filter(u -> u.getKycStatus() == KycStatus.APPROVED).count();
        long pending = users.stream().filter(u -> u.getKycStatus() == KycStatus.PENDING).count();
        long rejected = users.stream().filter(u -> u.getKycStatus() == KycStatus.REJECTED).count();
        long total = users.size();

        List<PieSlice> slices = new ArrayList<>();
        if (approved > 0) slices.add(kycSlice("KYC Approved", approved, total, "#10B981"));
        if (pending > 0) slices.add(kycSlice("KYC Pending", pending, total, "#F59E0B"));
        if (rejected > 0) slices.add(kycSlice("KYC Rejected", rejected, total, "#EF4444"));
        return slices;
    }

    private PieSlice kycSlice(String label, long count, long total, String color) {
        BigDecimal pct = total > 0
                ? BigDecimal.valueOf(count)
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new PieSlice(label, count, pct, color);
    }

    private List<MonthlyBarPoint> buildMonthlyFundsRaised(List<Property> properties) {
        // Group property createdAt months to approximate when funds were raised
        // More accurately: sum totalRaised for properties funded each month
        Map<YearMonth, BigDecimal> monthlyMap = last12MonthsMap();
        for (Property p : properties) {
            if (p.getFundedAt() != null && p.getTotalRaised() != null) {
                YearMonth ym = YearMonth.from(p.getFundedAt().atZone(ZoneOffset.UTC));
                monthlyMap.computeIfPresent(ym, (k, v) -> v.add(p.getTotalRaised()));
            }
        }
        return toBarPoints(monthlyMap);
    }

    private List<MonthlyBarPoint> buildMonthlyRoiPaid(List<MonthlyPaymentRun> monthlyRuns) {
        Map<YearMonth, BigDecimal> monthlyMap = last12MonthsMap();
        Instant chartSince = Instant.now().minus(400, ChronoUnit.DAYS);
        for (MonthlyPaymentRun run : monthlyRuns) {
            if (run.getAccrualEnd() != null
                    && !run.getAccrualEnd().isBefore(chartSince)
                    && run.getTotalDistributed() != null) {
                YearMonth ym = YearMonth.from(run.getAccrualEnd().atZone(ZoneOffset.UTC));
                monthlyMap.computeIfPresent(ym, (k, v) -> v.add(run.getTotalDistributed()));
            }
        }
        return toBarPoints(monthlyMap);
    }

    private List<MonthlyBarPoint> buildMonthlyNewUsers(List<User> users) {
        Map<YearMonth, BigDecimal> monthlyMap = last12MonthsMap();
        for (User u : users) {
            if (u.getCreatedAt() != null) {
                YearMonth ym = YearMonth.from(u.getCreatedAt().atZone(ZoneOffset.UTC));
                monthlyMap.computeIfPresent(ym, (k, v) -> v.add(BigDecimal.ONE));
            }
        }
        return toBarPoints(monthlyMap);
    }

    private List<SaleVoteItem> buildActiveSaleVoteDetails(List<Property> properties) {
        List<SaleVoteItem> items = new ArrayList<>();
        for (Property p : properties) {
            if (p.getSaleVotePercent().compareTo(BigDecimal.ZERO) <= 0) continue;

            String proposalStatus = proposalRepository.findByPropertyId(p.getId())
                    .map(prop -> prop.getStatus().name())
                    .orElse(null);

            items.add(new SaleVoteItem(
                    p.getId(),
                    p.getName(),
                    p.getSaleVotePercent(),
                    p.getSaleThresholdPercent(),
                    p.getSaleVoteOptInCount(),
                    p.getCurrentInvestors() != null ? p.getCurrentInvestors() : 0,
                    proposalStatus));
        }
        return items;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private Map<YearMonth, BigDecimal> last12MonthsMap() {
        Map<YearMonth, BigDecimal> map = new LinkedHashMap<>();
        YearMonth current = YearMonth.from(Instant.now().atZone(ZoneOffset.UTC));
        for (int i = 11; i >= 0; i--) {
            map.put(current.minusMonths(i), BigDecimal.ZERO);
        }
        return map;
    }

    private List<MonthlyBarPoint> toBarPoints(Map<YearMonth, BigDecimal> map) {
        List<MonthlyBarPoint> points = new ArrayList<>();
        for (Map.Entry<YearMonth, BigDecimal> entry : map.entrySet()) {
            YearMonth ym = entry.getKey();
            BigDecimal val = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            points.add(new MonthlyBarPoint(
                    ym.getMonth().name().substring(0, 3) + " " + ym.getYear(),
                    val,
                    val.longValue()));
        }
        return points;
    }
}
