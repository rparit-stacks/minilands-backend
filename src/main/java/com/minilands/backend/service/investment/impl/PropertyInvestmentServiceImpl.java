package com.minilands.backend.service.investment.impl;

import com.minilands.backend.dto.investment.BuySharesRequest;
import com.minilands.backend.dto.investment.HoldingDetailResponse;
import com.minilands.backend.dto.investment.PortfolioSummaryResponse;
import com.minilands.backend.dto.investment.RoiEarningResponse;
import com.minilands.backend.dto.investment.SellSharesRequest;
import com.minilands.backend.dto.investment.SharePriceResponse;
import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.PropertyHolding;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.Wallet;
import com.minilands.backend.entity.enums.HoldingStatus;
import com.minilands.backend.entity.enums.NotificationType;
import com.minilands.backend.entity.enums.PropertyStatus;
import com.minilands.backend.entity.enums.TransactionType;
import com.minilands.backend.repository.PropertyHoldingRepository;
import com.minilands.backend.repository.PropertyRepository;
import com.minilands.backend.repository.RoiEarningRepository;
import com.minilands.backend.repository.UserRepository;
import com.minilands.backend.service.investment.PropertyInvestmentService;
import com.minilands.backend.service.kyc.KycGuard;
import com.minilands.backend.service.notification.NotificationService;
import com.minilands.backend.service.property.SharePriceValuationService;
import com.minilands.backend.service.wallet.WalletLedgerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class PropertyInvestmentServiceImpl implements PropertyInvestmentService {

    private static final Set<PropertyStatus> TRADABLE_STATUSES = EnumSet.of(PropertyStatus.OPEN, PropertyStatus.ACTIVE);

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyHoldingRepository holdingRepository;
    private final RoiEarningRepository roiEarningRepository;
    private final WalletLedgerService walletLedgerService;
    private final SharePriceValuationService sharePriceValuationService;
    private final NotificationService notificationService;

    public PropertyInvestmentServiceImpl(
            UserRepository userRepository,
            PropertyRepository propertyRepository,
            PropertyHoldingRepository holdingRepository,
            RoiEarningRepository roiEarningRepository,
            WalletLedgerService walletLedgerService,
            SharePriceValuationService sharePriceValuationService,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.holdingRepository = holdingRepository;
        this.roiEarningRepository = roiEarningRepository;
        this.walletLedgerService = walletLedgerService;
        this.sharePriceValuationService = sharePriceValuationService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public HoldingDetailResponse buyShares(String userId, BuySharesRequest request) {
        User user = requireUser(userId);
        KycGuard.requireApproved(user);

        Property property = requireProperty(request.propertyId());
        assertTradable(property);

        BigDecimal sharePrice = sharePriceValuationService.getEstimatedSharePrice(property);
        BigDecimal shares = request.shares().setScale(4, RoundingMode.HALF_UP);
        BigDecimal amount = sharePrice.multiply(shares).setScale(2, RoundingMode.HALF_UP);

        assertSharesAvailable(property, shares);
        assertMaxSharesPerInvestor(property, userId, shares);

        Wallet wallet = walletLedgerService.requireWallet(userId);
        walletLedgerService.debit(
                wallet,
                amount,
                TransactionType.INVESTMENT,
                "Buy " + shares + " shares in " + property.getName(),
                property.getId());

        PropertyHolding holding = holdingRepository
                .findByUserIdAndPropertyId(userId, property.getId())
                .filter(h -> h.getStatus() == HoldingStatus.ACTIVE)
                .orElse(null);

        Instant now = Instant.now();
        if (holding == null) {
            holding = new PropertyHolding();
            holding.setUserId(userId);
            holding.setPropertyId(property.getId());
            holding.setSharesOwned(shares);
            holding.setInvestmentAmount(amount);
            holding.setCostBasis(amount);
            holding.setCurrentValue(amount);
            holding.setRoiEarned(BigDecimal.ZERO);
            holding.setStatus(HoldingStatus.ACTIVE);
            holding.setEntryDate(now);
            holding.setCreatedAt(now);
            incrementInvestorCount(property);
        } else {
            holding.setSharesOwned(holding.getSharesOwned().add(shares));
            holding.setInvestmentAmount(holding.getInvestmentAmount().add(amount));
            holding.setCostBasis(holding.getCostBasis().add(amount));
        }
        holding.setCurrentValue(holding.getSharesOwned().multiply(sharePrice).setScale(2, RoundingMode.HALF_UP));
        holding.setUpdatedAt(now);
        holdingRepository.save(holding);

        updatePropertyAfterBuy(property, shares, amount, sharePrice, now);

        notificationService.send(
                userId,
                NotificationType.INVESTMENT,
                "Investment confirmed",
                "You bought " + shares + " shares in " + property.getName() + " for ₹" + amount);

        return toHoldingDetail(holding, property);
    }

    @Override
    @Transactional
    public HoldingDetailResponse sellShares(String userId, SellSharesRequest request) {
        User user = requireUser(userId);
        KycGuard.requireApproved(user);

        Property property = requireProperty(request.propertyId());
        assertTradable(property);

        PropertyHolding holding = holdingRepository
                .findByUserIdAndPropertyId(userId, property.getId())
                .filter(h -> h.getStatus() == HoldingStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("No active holding for this property"));

        BigDecimal sharePrice = sharePriceValuationService.getEstimatedSharePrice(property);
        BigDecimal shares = request.shares().setScale(4, RoundingMode.HALF_UP);

        if (holding.getSharesOwned().compareTo(shares) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient shares. You own " + holding.getSharesOwned() + ", tried to sell " + shares);
        }

        BigDecimal sellAmount = sharePrice.multiply(shares).setScale(2, RoundingMode.HALF_UP);

        Wallet wallet = walletLedgerService.requireWallet(userId);
        walletLedgerService.credit(
                wallet,
                sellAmount,
                TransactionType.SALE,
                "Sell " + shares + " shares in " + property.getName(),
                property.getId());

        Instant now = Instant.now();
        BigDecimal ownedBeforeSale = holding.getSharesOwned();
        BigDecimal costReduction = proportionalCostReduction(holding.getCostBasis(), shares, ownedBeforeSale);
        holding.setSharesOwned(ownedBeforeSale.subtract(shares));
        holding.setCostBasis(holding.getCostBasis().subtract(costReduction).max(BigDecimal.ZERO));
        holding.setCurrentValue(holding.getSharesOwned().multiply(sharePrice).setScale(2, RoundingMode.HALF_UP));
        holding.setUpdatedAt(now);

        if (holding.getSharesOwned().compareTo(BigDecimal.ZERO) <= 0) {
            holding.setSharesOwned(BigDecimal.ZERO);
            holding.setStatus(HoldingStatus.WITHDRAWN);
            holding.setWithdrawnAt(now);
            holding.setCurrentValue(BigDecimal.ZERO);
            decrementInvestorCount(property);
        }
        holdingRepository.save(holding);

        updatePropertyAfterSell(property, shares, sellAmount, sharePrice, now);

        notificationService.send(
                userId,
                NotificationType.INVESTMENT,
                "Shares sold",
                "You sold " + shares + " shares in " + property.getName() + " for ₹" + sellAmount);

        return toHoldingDetail(holding, property);
    }

    @Override
    public List<HoldingDetailResponse> getHoldings(String userId) {
        return holdingRepository.findByUserIdAndStatus(userId, HoldingStatus.ACTIVE).stream()
                .map(h -> toHoldingDetail(h, propertyRepository.findById(h.getPropertyId()).orElse(null)))
                .filter(h -> h.propertyName() != null)
                .toList();
    }

    @Override
    public HoldingDetailResponse getHolding(String userId, String holdingId) {
        PropertyHolding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new IllegalArgumentException("Holding not found"));
        if (!holding.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Holding not found");
        }
        Property property = propertyRepository.findById(holding.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
        return toHoldingDetail(holding, property);
    }

    @Override
    public List<RoiEarningResponse> getRoiHistory(String userId, String holdingId) {
        PropertyHolding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new IllegalArgumentException("Holding not found"));
        if (!holding.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Holding not found");
        }
        return roiEarningRepository.findByHoldingId(holdingId).stream()
                .sorted((a, b) -> {
                    if (a.getEarnedOnDate() == null) return 1;
                    if (b.getEarnedOnDate() == null) return -1;
                    return b.getEarnedOnDate().compareTo(a.getEarnedOnDate());
                })
                .map(e -> new RoiEarningResponse(
                        e.getId(),
                        e.getRoiDistributionId(),
                        e.getAmount(),
                        e.getRoiPercentage(),
                        e.getEarnedOnDate(),
                        e.getCreatedAt()))
                .toList();
    }

    @Override
    public PortfolioSummaryResponse getPortfolioSummary(String userId) {
        List<PropertyHolding> allHoldings = holdingRepository.findByUserId(userId);
        List<PropertyHolding> active = allHoldings.stream()
                .filter(h -> h.getStatus() == HoldingStatus.ACTIVE)
                .toList();

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal currentPortfolioValue = BigDecimal.ZERO;

        for (PropertyHolding h : active) {
            BigDecimal cost = h.getCostBasis() != null ? h.getCostBasis() : BigDecimal.ZERO;
            totalInvested = totalInvested.add(cost);

            Property property = propertyRepository.findById(h.getPropertyId()).orElse(null);
            if (property != null && h.getSharesOwned() != null) {
                BigDecimal price = sharePriceValuationService.getEstimatedSharePrice(property);
                currentPortfolioValue = currentPortfolioValue.add(
                        h.getSharesOwned().multiply(price).setScale(2, RoundingMode.HALF_UP));
            }
        }

        BigDecimal totalRoiEarned = roiEarningRepository.findByUserIdOrderByEarnedOnDateDesc(userId).stream()
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal unrealizedGain = currentPortfolioValue.subtract(totalInvested);
        BigDecimal unrealizedGainPercent = totalInvested.compareTo(BigDecimal.ZERO) > 0
                ? unrealizedGain.multiply(BigDecimal.valueOf(100))
                        .divide(totalInvested, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new PortfolioSummaryResponse(
                allHoldings.size(),
                active.size(),
                totalInvested.setScale(2, RoundingMode.HALF_UP),
                currentPortfolioValue,
                totalRoiEarned.setScale(2, RoundingMode.HALF_UP),
                unrealizedGain.setScale(2, RoundingMode.HALF_UP),
                unrealizedGainPercent);
    }

    @Override
    public SharePriceResponse getSharePrice(String propertyId) {
        Property property = requireProperty(propertyId);
        BigDecimal estimated = sharePriceValuationService.getEstimatedSharePrice(property);
        return new SharePriceResponse(
                property.getId(),
                property.getName(),
                sharePriceValuationService.getBaseSharePrice(property),
                estimated,
                property.getAnnualRoi(),
                property.getLastValuationDate(),
                sharePriceValuationService.getNextValuationDate(property));
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Property requireProperty(String propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
    }

    private void assertTradable(Property property) {
        if (!TRADABLE_STATUSES.contains(property.getStatus())) {
            throw new IllegalArgumentException("Property is not open for trading. Status: " + property.getStatus());
        }
    }

    private void assertSharesAvailable(Property property, BigDecimal shares) {
        BigDecimal sold = property.getSharesSold() != null ? property.getSharesSold() : BigDecimal.ZERO;
        BigDecimal remaining = BigDecimal.valueOf(property.getTotalShares()).subtract(sold);
        if (remaining.compareTo(shares) < 0) {
            throw new IllegalArgumentException(
                    "Not enough shares available. Remaining: " + remaining + ", requested: " + shares);
        }
    }

    private void assertMaxSharesPerInvestor(Property property, String userId, BigDecimal newShares) {
        if (property.getMaxSharesPerInvestor() == null) {
            return;
        }
        BigDecimal max = BigDecimal.valueOf(property.getMaxSharesPerInvestor());
        BigDecimal existing = holdingRepository.findByUserIdAndPropertyId(userId, property.getId())
                .filter(h -> h.getStatus() == HoldingStatus.ACTIVE)
                .map(PropertyHolding::getSharesOwned)
                .orElse(BigDecimal.ZERO);
        if (existing.add(newShares).compareTo(max) > 0) {
            throw new IllegalArgumentException("Exceeds max shares per investor (" + property.getMaxSharesPerInvestor() + ")");
        }
    }

    private void updatePropertyAfterBuy(Property property, BigDecimal shares, BigDecimal amount, BigDecimal sharePrice, Instant now) {
        BigDecimal sold = property.getSharesSold() != null ? property.getSharesSold() : BigDecimal.ZERO;
        property.setSharesSold(sold.add(shares));
        BigDecimal raised = property.getTotalRaised() != null ? property.getTotalRaised() : BigDecimal.ZERO;
        property.setTotalRaised(raised.add(amount));
        property.setCurrentPrice(sharePrice);
        property.setUpdatedAt(now);
        propertyRepository.save(property);
    }

    private void updatePropertyAfterSell(Property property, BigDecimal shares, BigDecimal amount, BigDecimal sharePrice, Instant now) {
        BigDecimal sold = property.getSharesSold() != null ? property.getSharesSold() : BigDecimal.ZERO;
        property.setSharesSold(sold.subtract(shares).max(BigDecimal.ZERO));
        BigDecimal raised = property.getTotalRaised() != null ? property.getTotalRaised() : BigDecimal.ZERO;
        property.setTotalRaised(raised.subtract(amount).max(BigDecimal.ZERO));
        property.setCurrentPrice(sharePrice);
        property.setUpdatedAt(now);
        propertyRepository.save(property);
    }

    private void incrementInvestorCount(Property property) {
        property.setCurrentInvestors((property.getCurrentInvestors() != null ? property.getCurrentInvestors() : 0) + 1);
    }

    private void decrementInvestorCount(Property property) {
        int count = property.getCurrentInvestors() != null ? property.getCurrentInvestors() : 0;
        property.setCurrentInvestors(Math.max(0, count - 1));
    }

    private BigDecimal proportionalCostReduction(BigDecimal costBasis, BigDecimal sharesSold, BigDecimal totalOwned) {
        if (costBasis == null || totalOwned.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal ratio = sharesSold.divide(totalOwned, 10, RoundingMode.HALF_UP);
        return costBasis.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
    }

    private HoldingDetailResponse toHoldingDetail(PropertyHolding holding, Property property) {
        if (property == null) {
            return null;
        }
        BigDecimal currentSharePrice = sharePriceValuationService.getEstimatedSharePrice(property);
        BigDecimal shares = holding.getSharesOwned() != null ? holding.getSharesOwned() : BigDecimal.ZERO;
        BigDecimal currentValue = shares.multiply(currentSharePrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal costBasis = holding.getCostBasis() != null ? holding.getCostBasis() : BigDecimal.ZERO;
        BigDecimal rentalEarnings = holding.getRoiEarned() != null ? holding.getRoiEarned() : BigDecimal.ZERO;
        BigDecimal profitOrLoss = currentValue.subtract(costBasis);

        return new HoldingDetailResponse(
                holding.getId(),
                property.getId(),
                property.getName(),
                shares,
                costBasis,
                currentSharePrice,
                property.getAnnualRoi(),
                property.getLastValuationDate(),
                sharePriceValuationService.getNextValuationDate(property),
                currentValue,
                rentalEarnings,
                profitOrLoss,
                holding.getStatus(),
                holding.getEntryDate());
    }
}
