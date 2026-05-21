package com.minilands.backend.service.property.impl;

import com.minilands.backend.dto.property.MonthlyPaymentDistributionResponse;
import com.minilands.backend.dto.property.MonthlyPaymentExecuteRequest;
import com.minilands.backend.dto.property.MonthlyPaymentHistoryItem;
import com.minilands.backend.dto.property.MonthlyPaymentPreviewInvestorRow;
import com.minilands.backend.dto.property.MonthlyPaymentPreviewResponse;
import com.minilands.backend.entity.MonthlyPaymentRun;
import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.PropertyHolding;
import com.minilands.backend.entity.RoiEarning;
import com.minilands.backend.entity.Wallet;
import com.minilands.backend.entity.enums.HoldingStatus;
import com.minilands.backend.entity.enums.NotificationType;
import com.minilands.backend.entity.enums.PropertyStatus;
import com.minilands.backend.entity.enums.TransactionType;
import com.minilands.backend.repository.MonthlyPaymentRunRepository;
import com.minilands.backend.repository.PropertyHoldingRepository;
import com.minilands.backend.repository.PropertyRepository;
import com.minilands.backend.repository.RoiEarningRepository;
import com.minilands.backend.service.notification.NotificationService;
import com.minilands.backend.service.property.MonthlyPaymentAccrualMath;
import com.minilands.backend.service.property.MonthlyPaymentDistributionService;
import com.minilands.backend.service.wallet.WalletLedgerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class MonthlyPaymentDistributionServiceImpl implements MonthlyPaymentDistributionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int MIN_ACCRUAL_SECONDS = 60;

    private final PropertyRepository propertyRepository;
    private final PropertyHoldingRepository holdingRepository;
    private final WalletLedgerService walletLedgerService;
    private final MonthlyPaymentRunRepository monthlyPaymentRunRepository;
    private final RoiEarningRepository roiEarningRepository;
    private final NotificationService notificationService;

    public MonthlyPaymentDistributionServiceImpl(
            PropertyRepository propertyRepository,
            PropertyHoldingRepository holdingRepository,
            WalletLedgerService walletLedgerService,
            MonthlyPaymentRunRepository monthlyPaymentRunRepository,
            RoiEarningRepository roiEarningRepository,
            NotificationService notificationService) {
        this.propertyRepository = propertyRepository;
        this.holdingRepository = holdingRepository;
        this.walletLedgerService = walletLedgerService;
        this.monthlyPaymentRunRepository = monthlyPaymentRunRepository;
        this.roiEarningRepository = roiEarningRepository;
        this.notificationService = notificationService;
    }

    @Override
    public MonthlyPaymentPreviewResponse preview(String propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
        requireRentConfigured(property);
        AccrualPlan plan = buildPlan(property, null, null, Instant.now());
        validatePlan(plan);

        List<WeightedHolding> positive = sortedPositiveHoldings(plan);
        AllocationPreview alloc = allocatePreview(plan.poolNet(), plan.denominatorShareDays(), plan.investorShareDaysSum(), positive);

        return new MonthlyPaymentPreviewResponse(
                propertyId,
                plan.accrualStart(),
                plan.accrualEnd(),
                plan.elapsedDays(),
                plan.monthlyAmount(),
                plan.poolGross(),
                plan.platformFeeAmount(),
                plan.poolNet(),
                plan.denominatorShareDays(),
                plan.investorShareDaysSum(),
                alloc.spvShareDayWeight(),
                alloc.spvPayout(),
                plan.denominatorShareDays(),
                alloc.investorRows());
    }

    @Override
    @Transactional
    public MonthlyPaymentDistributionResponse distribute(String propertyId, MonthlyPaymentExecuteRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
        requireActiveForMonthlyPayment(property);

        MonthlyPaymentExecuteRequest body = request != null ? request : new MonthlyPaymentExecuteRequest(null, null);
        Instant accrualEnd = Instant.now();
        AccrualPlan plan = buildPlan(
                property,
                body.monthlyAmountOverride(),
                body.platformFeePercentOverride(),
                accrualEnd);
        validatePlan(plan);

        List<WeightedHolding> positive = sortedPositiveHoldings(plan);
        AllocationPreview alloc = allocatePreview(plan.poolNet(), plan.denominatorShareDays(), plan.investorShareDaysSum(), positive);

        Instant now = Instant.now();
        MonthlyPaymentRun run = new MonthlyPaymentRun();
        run.setPropertyId(propertyId);
        run.setAccrualStart(plan.accrualStart());
        run.setAccrualEnd(plan.accrualEnd());
        run.setMonthlyAmountConfigured(plan.monthlyAmount());
        run.setPlatformFeePercent(plan.feePercent());
        run.setPoolGross(plan.poolGross());
        run.setPlatformFeeAmount(plan.platformFeeAmount());
        run.setPoolNet(plan.poolNet());
        run.setDenominatorShareDays(plan.denominatorShareDays());
        run.setInvestorShareDaysSum(plan.investorShareDaysSum());
        run.setTotalShareDayWeight(plan.denominatorShareDays());
        run.setSpvDistributed(ZERO);
        run.setTotalDistributed(ZERO);
        run.setInvestorsPaid(0);
        run.setCreatedAt(now);
        run = monthlyPaymentRunRepository.save(run);

        BigDecimal investorsOut = ZERO;
        int paid = 0;

        for (MonthlyPaymentPreviewInvestorRow row : alloc.investorRows()) {
            if (row.estimatedPayout() == null || row.estimatedPayout().compareTo(ZERO) <= 0) {
                continue;
            }
            PropertyHolding holding = holdingRepository.findById(row.holdingId())
                    .orElseThrow(() -> new IllegalStateException("Holding not found: " + row.holdingId()));
            Wallet wallet = walletLedgerService.requireWallet(holding.getUserId());
            walletLedgerService.credit(
                    wallet,
                    row.estimatedPayout(),
                    TransactionType.ROI,
                    "Monthly payment: " + property.getName() + " ("
                            + plan.accrualStart() + " → " + plan.accrualEnd() + ")",
                    propertyId);

            holding.setRoiEarned((holding.getRoiEarned() != null ? holding.getRoiEarned() : ZERO).add(row.estimatedPayout()));
            holding.setUpdatedAt(now);
            holdingRepository.save(holding);

            BigDecimal allocPct = plan.investorShareDaysSum().compareTo(ZERO) > 0
                    ? row.shareDayWeight()
                            .divide(plan.investorShareDaysSum(), 8, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(4, RoundingMode.HALF_UP)
                    : ZERO;

            RoiEarning earning = new RoiEarning();
            earning.setRoiDistributionId(null);
            earning.setMonthlyPaymentRunId(run.getId());
            earning.setHoldingId(holding.getId());
            earning.setUserId(holding.getUserId());
            earning.setPropertyId(propertyId);
            earning.setAmount(row.estimatedPayout());
            earning.setRoiPercentage(allocPct);
            earning.setEarnedOnDate(now);
            earning.setCreatedAt(now);
            roiEarningRepository.save(earning);

            investorsOut = investorsOut.add(row.estimatedPayout());
            paid++;

            notificationService.send(
                    holding.getUserId(),
                    NotificationType.ROI,
                    "Monthly payment received",
                    "₹" + row.estimatedPayout() + " credited to your wallet from " + property.getName()
                            + " (pro-rata among subscribed shares in this accrual window).");
        }

        BigDecimal spvOut = ZERO;

        BigDecimal totalOut = investorsOut.add(spvOut);
        run.setSpvDistributed(spvOut);
        run.setInvestorsPaid(paid);
        run.setTotalDistributed(totalOut);
        monthlyPaymentRunRepository.save(run);

        property.setLastMonthlyPaymentDistributedAt(plan.accrualEnd());
        property.setUpdatedAt(now);
        propertyRepository.save(property);

        return new MonthlyPaymentDistributionResponse(
                propertyId,
                plan.accrualStart(),
                plan.accrualEnd(),
                plan.elapsedDays(),
                plan.monthlyAmount(),
                plan.poolGross(),
                plan.platformFeeAmount(),
                plan.poolNet(),
                plan.denominatorShareDays(),
                paid,
                investorsOut,
                spvOut,
                totalOut);
    }

    @Override
    public List<MonthlyPaymentHistoryItem> history(String propertyId) {
        return monthlyPaymentRunRepository.findByPropertyIdOrderByCreatedAtDesc(propertyId).stream()
                .map(r -> new MonthlyPaymentHistoryItem(
                        r.getId(),
                        r.getAccrualStart(),
                        r.getAccrualEnd(),
                        r.getPoolGross(),
                        r.getPoolNet(),
                        r.getTotalDistributed(),
                        r.getSpvDistributed() != null ? r.getSpvDistributed() : ZERO,
                        r.getInvestorsPaid(),
                        r.getCreatedAt()))
                .toList();
    }

    private void validatePlan(AccrualPlan plan) {
        if (plan.poolNet().compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Net distribution pool is zero — widen the accrual window or increase the monthly amount.");
        }
        if (plan.denominatorShareDays().compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid cap table: totalShares × accrual days must be positive.");
        }
        if (plan.investorShareDaysSum().compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "No subscribed shares with eligible days in this accrual window — nothing to pay out.");
        }
        if (plan.investorShareDaysSum().compareTo(plan.denominatorShareDays()) > 0) {
            throw new IllegalStateException(
                    "Investor share-days exceed totalShares×window — check active holdings vs property.totalShares.");
        }
    }

    private List<WeightedHolding> sortedPositiveHoldings(AccrualPlan plan) {
        return plan.weightedHoldings().stream()
                .filter(w -> w.weight().compareTo(ZERO) > 0)
                .sorted(Comparator.comparing(w -> w.holding().getId()))
                .toList();
    }

    private AllocationPreview allocatePreview(
            BigDecimal poolNet,
            BigDecimal denominator,
            BigDecimal investorShareDaysSum,
            List<WeightedHolding> positive) {
        BigDecimal spvShareDays = denominator.subtract(investorShareDaysSum);
        if (spvShareDays.compareTo(ZERO) < 0) {
            spvShareDays = ZERO;
        }

        List<BigDecimal> weights = new ArrayList<>();
        for (WeightedHolding wh : positive) {
            weights.add(wh.weight());
        }

        List<BigDecimal> payouts = allocateProportional(poolNet, weights);

        BigDecimal spvPayout = ZERO;
        List<BigDecimal> investorPayouts = payouts;

        List<MonthlyPaymentPreviewInvestorRow> rows = new ArrayList<>();
        for (int i = 0; i < positive.size(); i++) {
            WeightedHolding w = positive.get(i);
            PropertyHolding h = w.holding();
            BigDecimal pay = i < investorPayouts.size() ? investorPayouts.get(i) : ZERO;
            rows.add(new MonthlyPaymentPreviewInvestorRow(
                    h.getId(),
                    h.getUserId(),
                    h.getSharesOwned(),
                    w.eligibleDays(),
                    w.weight(),
                    pay));
        }

        return new AllocationPreview(spvShareDays, spvPayout, rows);
    }

    /**
     * Split {@code poolNet} by weights; last bucket absorbs paise rounding so the sum matches {@code poolNet}.
     */
    private static List<BigDecimal> allocateProportional(BigDecimal poolNet, List<BigDecimal> weights) {
        if (weights.isEmpty()) {
            return List.of();
        }
        BigDecimal totalW = weights.stream().reduce(ZERO, BigDecimal::add);
        if (totalW.compareTo(ZERO) <= 0) {
            throw new IllegalStateException("Allocation weight sum must be positive.");
        }
        List<BigDecimal> out = new ArrayList<>();
        BigDecimal running = ZERO;
        for (int i = 0; i < weights.size(); i++) {
            boolean last = i == weights.size() - 1;
            BigDecimal payout = last
                    ? poolNet.subtract(running).setScale(2, RoundingMode.HALF_UP)
                    : poolNet.multiply(weights.get(i)).divide(totalW, 2, RoundingMode.HALF_UP);
            out.add(payout);
            running = running.add(payout);
        }
        return out;
    }

    private void requireRentConfigured(Property property) {
        BigDecimal monthly = property.getMonthlyRent();
        if (monthly == null || monthly.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Configure a positive monthly payment amount on the property first.");
        }
    }

    private void requireActiveForMonthlyPayment(Property property) {
        if (property.getStatus() != PropertyStatus.ACTIVE) {
            throw new IllegalArgumentException("Monthly payment is only available for ACTIVE properties.");
        }
        requireRentConfigured(property);
    }

    private AccrualPlan buildPlan(
            Property property,
            BigDecimal monthlyAmountOverride,
            Integer platformFeePercentOverride,
            Instant accrualEnd) {
        BigDecimal monthlyAmount = monthlyAmountOverride != null ? monthlyAmountOverride : property.getMonthlyRent();
        if (monthlyAmount == null || monthlyAmount.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Monthly payment amount must be positive.");
        }

        if (property.getTotalShares() == null || property.getTotalShares() <= 0) {
            throw new IllegalArgumentException("Property must have a positive totalShares for rental split.");
        }
        BigDecimal totalSharesBd = BigDecimal.valueOf(property.getTotalShares());

        List<PropertyHolding> holdings =
                holdingRepository.findByPropertyIdAndStatus(property.getId(), HoldingStatus.ACTIVE);

        Instant propertyReady = property.getFundedAt() != null ? property.getFundedAt() : property.getCreatedAt();
        Instant earliestEntry = holdings.stream()
                .map(h -> h.getEntryDate() != null ? h.getEntryDate() : h.getCreatedAt())
                .filter(Objects::nonNull)
                .min(Instant::compareTo)
                .orElse(null);

        Instant anchor;
        if (propertyReady == null && earliestEntry == null) {
            anchor = property.getCreatedAt() != null ? property.getCreatedAt() : accrualEnd;
        } else if (propertyReady == null) {
            anchor = earliestEntry;
        } else if (earliestEntry == null) {
            anchor = propertyReady;
        } else {
            anchor = propertyReady.compareTo(earliestEntry) >= 0 ? propertyReady : earliestEntry;
        }

        Instant accrualStart = property.getLastMonthlyPaymentDistributedAt() != null
                ? property.getLastMonthlyPaymentDistributedAt()
                : anchor;

        if (!accrualEnd.isAfter(accrualStart)) {
            throw new IllegalArgumentException("Accrual window is empty — accrualEnd must be after accrualStart.");
        }
        long seconds = java.time.Duration.between(accrualStart, accrualEnd).getSeconds();
        if (seconds < MIN_ACCRUAL_SECONDS) {
            throw new IllegalArgumentException(
                    "Accrual window is too short (minimum " + MIN_ACCRUAL_SECONDS + " seconds between runs).");
        }

        BigDecimal elapsedDays = MonthlyPaymentAccrualMath.elapsedDays(accrualStart, accrualEnd);
        BigDecimal denominator = totalSharesBd.multiply(elapsedDays);

        BigDecimal poolGross = MonthlyPaymentAccrualMath.poolGross(monthlyAmount, accrualStart, accrualEnd);

        int feePercent = platformFeePercentOverride != null
                ? platformFeePercentOverride
                : property.getRentPlatformFeePercent();
        BigDecimal platformFee = poolGross
                .multiply(BigDecimal.valueOf(feePercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal poolNet = poolGross.subtract(platformFee).max(ZERO);

        BigDecimal minHoldDays = MonthlyPaymentAccrualMath.minEligibleDays(property.getDistributionFrequency());

        List<WeightedHolding> weighted = new ArrayList<>();
        BigDecimal investorSum = ZERO;
        for (PropertyHolding h : holdings) {
            BigDecimal shares = h.getSharesOwned() != null ? h.getSharesOwned() : ZERO;
            if (shares.compareTo(ZERO) <= 0) {
                continue;
            }
            Instant entry = h.getEntryDate() != null ? h.getEntryDate() : h.getCreatedAt();
            Instant holdingEnd = h.getWithdrawnAt() != null && h.getWithdrawnAt().isBefore(accrualEnd)
                    ? h.getWithdrawnAt()
                    : accrualEnd;
            BigDecimal eligible = entry != null
                    ? MonthlyPaymentAccrualMath.eligibleOverlapDays(entry, holdingEnd, accrualStart, accrualEnd)
                    : ZERO;

            // Investor must have held for at least one full payout cycle (since entry) to be eligible
            BigDecimal totalHeldDays = entry != null
                    ? MonthlyPaymentAccrualMath.elapsedDays(entry, accrualEnd)
                    : ZERO;
            BigDecimal weight = totalHeldDays.compareTo(minHoldDays) >= 0
                    ? shares.multiply(eligible)
                    : ZERO;

            weighted.add(new WeightedHolding(h, eligible, weight));
            investorSum = investorSum.add(weight);
        }

        return new AccrualPlan(
                accrualStart,
                accrualEnd,
                elapsedDays,
                monthlyAmount,
                feePercent,
                poolGross,
                platformFee,
                poolNet,
                weighted,
                investorSum,
                denominator);
    }

    private record AccrualPlan(
            Instant accrualStart,
            Instant accrualEnd,
            BigDecimal elapsedDays,
            BigDecimal monthlyAmount,
            int feePercent,
            BigDecimal poolGross,
            BigDecimal platformFeeAmount,
            BigDecimal poolNet,
            List<WeightedHolding> weightedHoldings,
            BigDecimal investorShareDaysSum,
            BigDecimal denominatorShareDays) {
    }

    private record WeightedHolding(PropertyHolding holding, BigDecimal eligibleDays, BigDecimal weight) {
    }

    private record AllocationPreview(BigDecimal spvShareDayWeight, BigDecimal spvPayout, List<MonthlyPaymentPreviewInvestorRow> investorRows) {
    }
}
