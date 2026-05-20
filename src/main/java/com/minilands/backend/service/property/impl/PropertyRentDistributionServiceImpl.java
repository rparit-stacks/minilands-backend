package com.minilands.backend.service.property.impl;

import com.minilands.backend.dto.property.DistributeRentRequest;
import com.minilands.backend.dto.property.RentDistributionResponse;
import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.PropertyHolding;
import com.minilands.backend.entity.RoiDistribution;
import com.minilands.backend.entity.RoiEarning;
import com.minilands.backend.entity.Wallet;
import com.minilands.backend.entity.enums.HoldingStatus;
import com.minilands.backend.entity.enums.NotificationType;
import com.minilands.backend.entity.enums.RoiDistributionStatus;
import com.minilands.backend.entity.enums.TransactionType;
import com.minilands.backend.repository.PropertyHoldingRepository;
import com.minilands.backend.repository.PropertyRepository;
import com.minilands.backend.repository.RoiDistributionRepository;
import com.minilands.backend.repository.RoiEarningRepository;
import com.minilands.backend.service.notification.NotificationService;
import com.minilands.backend.service.property.PropertyRentDistributionService;
import com.minilands.backend.service.property.SharePriceValuationService;
import com.minilands.backend.service.wallet.WalletLedgerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class PropertyRentDistributionServiceImpl implements PropertyRentDistributionService {

    private final PropertyRepository propertyRepository;
    private final PropertyHoldingRepository holdingRepository;
    private final WalletLedgerService walletLedgerService;
    private final SharePriceValuationService sharePriceValuationService;
    private final RoiDistributionRepository roiDistributionRepository;
    private final RoiEarningRepository roiEarningRepository;
    private final NotificationService notificationService;

    public PropertyRentDistributionServiceImpl(
            PropertyRepository propertyRepository,
            PropertyHoldingRepository holdingRepository,
            WalletLedgerService walletLedgerService,
            SharePriceValuationService sharePriceValuationService,
            RoiDistributionRepository roiDistributionRepository,
            RoiEarningRepository roiEarningRepository,
            NotificationService notificationService) {
        this.propertyRepository = propertyRepository;
        this.holdingRepository = holdingRepository;
        this.walletLedgerService = walletLedgerService;
        this.sharePriceValuationService = sharePriceValuationService;
        this.roiDistributionRepository = roiDistributionRepository;
        this.roiEarningRepository = roiEarningRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public RentDistributionResponse distributeRent(String propertyId, DistributeRentRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        BigDecimal monthlyRent = request.monthlyRent() != null
                ? request.monthlyRent()
                : property.getMonthlyRent();
        if (monthlyRent == null || monthlyRent.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("monthlyRent must be set on the property or in the request");
        }

        // Priority: request override → property-level fee → global default (10%)
        int feePercent = request.platformFeePercent() != null
                ? request.platformFeePercent()
                : property.getRentPlatformFeePercent();
        BigDecimal platformFee = monthlyRent
                .multiply(BigDecimal.valueOf(feePercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal distributableRent = monthlyRent.subtract(platformFee);

        List<PropertyHolding> holdings = holdingRepository.findByPropertyIdAndStatus(propertyId, HoldingStatus.ACTIVE);
        if (holdings.isEmpty()) {
            throw new IllegalArgumentException("No active investors for this property");
        }

        YearMonth period = (request.year() != null && request.month() != null)
                ? YearMonth.of(request.year(), request.month())
                : YearMonth.now(ZoneOffset.UTC);
        if (roiDistributionRepository
                .findByPropertyIdAndDistributionYearAndDistributionMonth(
                        propertyId, period.getYear(), period.getMonthValue())
                .isPresent()) {
            throw new IllegalArgumentException("Rent already distributed for " + period);
        }

        // Current share price — all holdings valued at same price
        BigDecimal currentSharePrice = sharePriceValuationService.getEstimatedSharePrice(property);

        // Total current value of all active holdings combined
        BigDecimal totalPortfolioValue = holdings.stream()
                .map(h -> h.getSharesOwned() != null
                        ? h.getSharesOwned().multiply(currentSharePrice)
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPortfolioValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Total portfolio value is zero — cannot distribute");
        }

        Instant now = Instant.now();

        RoiDistribution distribution = new RoiDistribution();
        distribution.setPropertyId(propertyId);
        distribution.setDistributionYear(period.getYear());
        distribution.setDistributionMonth(period.getMonthValue());
        distribution.setRoiPercentage(distributableRent
                .divide(totalPortfolioValue, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP));
        distribution.setTotalDistributed(BigDecimal.ZERO);
        distribution.setStatus(RoiDistributionStatus.COMPLETED);
        distribution.setDistributedAt(now);
        distribution.setCreatedAt(now);
        distribution = roiDistributionRepository.save(distribution);

        BigDecimal totalDistributed = BigDecimal.ZERO;
        int investorsPaid = 0;

        for (PropertyHolding holding : holdings) {
            BigDecimal holdingValue = holding.getSharesOwned() != null
                    ? holding.getSharesOwned().multiply(currentSharePrice)
                    : BigDecimal.ZERO;
            if (holdingValue.compareTo(BigDecimal.ZERO) <= 0) continue;

            // Payout proportional to current value — not share count
            BigDecimal ownershipFraction = holdingValue.divide(totalPortfolioValue, 10, RoundingMode.HALF_UP);
            BigDecimal payout = distributableRent.multiply(ownershipFraction).setScale(2, RoundingMode.HALF_UP);
            if (payout.compareTo(BigDecimal.ZERO) <= 0) continue;

            Wallet wallet = walletLedgerService.requireWallet(holding.getUserId());
            walletLedgerService.credit(
                    wallet,
                    payout,
                    TransactionType.ROI,
                    "Rental income: " + property.getName() + " (" + period.getMonth().name().substring(0, 3) + " " + period.getYear() + ")",
                    propertyId);

            holding.setRoiEarned((holding.getRoiEarned() != null ? holding.getRoiEarned() : BigDecimal.ZERO).add(payout));
            holding.setUpdatedAt(now);
            holdingRepository.save(holding);

            RoiEarning earning = new RoiEarning();
            earning.setRoiDistributionId(distribution.getId());
            earning.setHoldingId(holding.getId());
            earning.setUserId(holding.getUserId());
            earning.setPropertyId(propertyId);
            earning.setAmount(payout);
            earning.setRoiPercentage(distribution.getRoiPercentage());
            earning.setEarnedOnDate(now);
            earning.setCreatedAt(now);
            roiEarningRepository.save(earning);

            totalDistributed = totalDistributed.add(payout);
            investorsPaid++;

            notificationService.send(
                    holding.getUserId(),
                    NotificationType.ROI,
                    "Rental payout received",
                    "₹" + payout + " credited to your wallet from " + property.getName()
                            + ". Your holding value: ₹" + holdingValue.setScale(0, RoundingMode.HALF_UP));
        }

        distribution.setTotalDistributed(totalDistributed);
        roiDistributionRepository.save(distribution);

        return new RentDistributionResponse(
                propertyId,
                monthlyRent,
                platformFee,
                distributableRent,
                investorsPaid,
                totalDistributed);
    }
}
