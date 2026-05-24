package com.minilands.backend.service.marketplace.impl;

import com.minilands.backend.config.MarketplaceProperties;
import com.minilands.backend.dto.marketplace.BuyListedSharesRequest;
import com.minilands.backend.dto.marketplace.ListSharesRequest;
import com.minilands.backend.dto.marketplace.MarketplaceListingResponse;
import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.PropertyHolding;
import com.minilands.backend.entity.ShareListing;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.Wallet;
import com.minilands.backend.entity.enums.HoldingStatus;
import com.minilands.backend.entity.enums.MarketplaceListingStatus;
import com.minilands.backend.entity.enums.NotificationType;
import com.minilands.backend.entity.enums.TransactionType;
import com.minilands.backend.repository.PropertyHoldingRepository;
import com.minilands.backend.repository.PropertyRepository;
import com.minilands.backend.repository.ShareListingRepository;
import com.minilands.backend.repository.UserRepository;
import com.minilands.backend.service.kyc.KycGuard;
import com.minilands.backend.service.marketplace.ShareMarketplaceService;
import com.minilands.backend.service.notification.NotificationService;
import com.minilands.backend.service.property.SharePriceValuationService;
import com.minilands.backend.service.wallet.WalletLedgerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class ShareMarketplaceServiceImpl implements ShareMarketplaceService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyHoldingRepository holdingRepository;
    private final ShareListingRepository shareListingRepository;
    private final WalletLedgerService walletLedgerService;
    private final SharePriceValuationService sharePriceValuationService;
    private final NotificationService notificationService;
    private final MarketplaceProperties marketplaceProperties;

    public ShareMarketplaceServiceImpl(
            UserRepository userRepository,
            PropertyRepository propertyRepository,
            PropertyHoldingRepository holdingRepository,
            ShareListingRepository shareListingRepository,
            WalletLedgerService walletLedgerService,
            SharePriceValuationService sharePriceValuationService,
            NotificationService notificationService,
            MarketplaceProperties marketplaceProperties) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.holdingRepository = holdingRepository;
        this.shareListingRepository = shareListingRepository;
        this.walletLedgerService = walletLedgerService;
        this.sharePriceValuationService = sharePriceValuationService;
        this.notificationService = notificationService;
        this.marketplaceProperties = marketplaceProperties;
    }

    @Override
    @Transactional
    public MarketplaceListingResponse listShares(String sellerId, ListSharesRequest request) {
        User seller = requireUser(sellerId);
        KycGuard.requireApproved(seller);

        Property property = requireProperty(request.propertyId());

        PropertyHolding holding = holdingRepository
                .findByUserIdAndPropertyId(sellerId, property.getId())
                .filter(h -> h.getStatus() == HoldingStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("No active holding for this property"));

        enforceMinHoldPeriod(property, holding);

        BigDecimal sharesToList = request.shares().setScale(4, RoundingMode.HALF_UP);

        BigDecimal alreadyListed = shareListingRepository
                .findBySellerIdAndStatus(sellerId, MarketplaceListingStatus.ACTIVE)
                .stream()
                .filter(l -> l.getPropertyId().equals(property.getId()))
                .map(ShareListing::getSharesListed)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableToList = holding.getSharesOwned().subtract(alreadyListed);
        if (availableToList.compareTo(sharesToList) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient unlisted shares. Available to list: " + availableToList + ", requested: " + sharesToList);
        }

        BigDecimal askPricePerShare = request.askPricePerShare().setScale(2, RoundingMode.HALF_UP);
        BigDecimal marketPricePerShare = sharePriceValuationService.getEstimatedSharePrice(property);

        // Floor: seller cannot list below their own cost basis per share (prevents panic-selling at a loss that misrepresents the asset)
        BigDecimal costBasisPerShare = BigDecimal.ZERO;
        if (holding.getCostBasis() != null && holding.getSharesOwned().compareTo(BigDecimal.ZERO) > 0) {
            costBasisPerShare = holding.getCostBasis()
                    .divide(holding.getSharesOwned(), 2, RoundingMode.HALF_UP);
        }
        if (askPricePerShare.compareTo(costBasisPerShare) < 0) {
            throw new IllegalArgumentException(
                    "Ask price ₹" + askPricePerShare + " is below your cost basis of ₹" + costBasisPerShare + " per share. Minimum ask price is ₹" + costBasisPerShare + ".");
        }

        BigDecimal totalAsk = askPricePerShare.multiply(sharesToList).setScale(2, RoundingMode.HALF_UP);

        Instant now = Instant.now();
        ShareListing listing = new ShareListing();
        listing.setSellerId(sellerId);
        listing.setPropertyId(property.getId());
        listing.setHoldingId(holding.getId());
        listing.setSharesListed(sharesToList);
        listing.setPricePerShare(askPricePerShare);
        listing.setTotalAskPrice(totalAsk);
        listing.setStatus(MarketplaceListingStatus.ACTIVE);
        listing.setExpiresAt(now.plus(Duration.ofDays(marketplaceProperties.getListingExpiryDays())));
        listing.setCreatedAt(now);
        listing.setUpdatedAt(now);
        shareListingRepository.save(listing);

        BigDecimal discountVsMarket = marketPricePerShare.subtract(askPricePerShare).setScale(2, RoundingMode.HALF_UP);
        String priceContext = discountVsMarket.compareTo(BigDecimal.ZERO) > 0
                ? " (₹" + discountVsMarket + " below market — buyers get an instant deal)"
                : discountVsMarket.compareTo(BigDecimal.ZERO) < 0
                ? " (₹" + discountVsMarket.abs() + " above market price)"
                : " (at market price)";

        notificationService.send(
                sellerId,
                NotificationType.INVESTMENT,
                "Shares listed",
                sharesToList + " shares in " + property.getName() + " listed at ₹" + askPricePerShare + "/share" + priceContext + ". Waiting for a buyer.");

        return toResponse(listing, property.getName());
    }

    @Override
    @Transactional
    public MarketplaceListingResponse buyListing(String buyerId, BuyListedSharesRequest request) {
        User buyer = requireUser(buyerId);
        KycGuard.requireApproved(buyer);

        ShareListing listing = shareListingRepository.findById(request.listingId())
                .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        if (listing.getStatus() != MarketplaceListingStatus.ACTIVE) {
            throw new IllegalArgumentException("Listing is no longer available. Status: " + listing.getStatus());
        }
        if (listing.getExpiresAt() != null && Instant.now().isAfter(listing.getExpiresAt())) {
            // Lazy-expire on read so a buyer never trades against a stale listing the sweeper hasn't reached yet.
            listing.setStatus(MarketplaceListingStatus.EXPIRED);
            listing.setExpiredAt(Instant.now());
            listing.setUpdatedAt(Instant.now());
            shareListingRepository.save(listing);
            throw new IllegalArgumentException("Listing has expired and is no longer available.");
        }
        if (listing.getSellerId().equals(buyerId)) {
            throw new IllegalArgumentException("You cannot buy your own listing");
        }

        Property property = requireProperty(listing.getPropertyId());

        BigDecimal gross = listing.getTotalAskPrice();
        BigDecimal feePercent = property.getMarketplaceFeePercent();
        BigDecimal platformFee = gross
                .multiply(feePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal sellerProceeds = gross.subtract(platformFee).max(BigDecimal.ZERO);

        // Debit buyer wallet (buyer always pays full ask)
        Wallet buyerWallet = walletLedgerService.requireWallet(buyerId);
        walletLedgerService.debit(
                buyerWallet,
                gross,
                TransactionType.INVESTMENT,
                "Buy " + listing.getSharesListed() + " shares in " + property.getName() + " from marketplace",
                listing.getId());

        // Credit seller wallet (proceeds = ask − platform fee). Fee is retained by the platform off-flow.
        Wallet sellerWallet = walletLedgerService.requireWallet(listing.getSellerId());
        String saleNote = platformFee.compareTo(BigDecimal.ZERO) > 0
                ? "Sold " + listing.getSharesListed() + " shares in " + property.getName() + " via marketplace (₹" + platformFee + " platform fee deducted)"
                : "Sold " + listing.getSharesListed() + " shares in " + property.getName() + " via marketplace";
        walletLedgerService.credit(
                sellerWallet,
                sellerProceeds,
                TransactionType.SALE,
                saleNote,
                listing.getId());

        // Reduce seller holding
        PropertyHolding sellerHolding = holdingRepository.findById(listing.getHoldingId())
                .orElseThrow(() -> new IllegalArgumentException("Seller holding not found"));

        Instant now = Instant.now();
        BigDecimal shares = listing.getSharesListed();
        BigDecimal marketPricePerShare = sharePriceValuationService.getEstimatedSharePrice(property);

        BigDecimal costReduction = proportionalCostReduction(sellerHolding.getCostBasis(), shares, sellerHolding.getSharesOwned());
        sellerHolding.setSharesOwned(sellerHolding.getSharesOwned().subtract(shares));
        sellerHolding.setCostBasis(sellerHolding.getCostBasis().subtract(costReduction).max(BigDecimal.ZERO));
        sellerHolding.setCurrentValue(sellerHolding.getSharesOwned().multiply(marketPricePerShare).setScale(2, RoundingMode.HALF_UP));
        sellerHolding.setUpdatedAt(now);
        if (sellerHolding.getSharesOwned().compareTo(BigDecimal.ZERO) <= 0) {
            sellerHolding.setSharesOwned(BigDecimal.ZERO);
            sellerHolding.setStatus(HoldingStatus.WITHDRAWN);
            sellerHolding.setWithdrawnAt(now);
            sellerHolding.setCurrentValue(BigDecimal.ZERO);
            decrementInvestorCount(property);
        }
        holdingRepository.save(sellerHolding);

        // Create or update buyer holding
        PropertyHolding buyerHolding = holdingRepository
                .findByUserIdAndPropertyId(buyerId, property.getId())
                .filter(h -> h.getStatus() == HoldingStatus.ACTIVE)
                .orElse(null);

        // Buyer's cost basis = what they actually paid (gross of platform fee).
        if (buyerHolding == null) {
            buyerHolding = new PropertyHolding();
            buyerHolding.setUserId(buyerId);
            buyerHolding.setPropertyId(property.getId());
            buyerHolding.setSharesOwned(shares);
            buyerHolding.setInvestmentAmount(gross);
            buyerHolding.setCostBasis(gross);
            buyerHolding.setCurrentValue(gross);
            buyerHolding.setRoiEarned(BigDecimal.ZERO);
            buyerHolding.setStatus(HoldingStatus.ACTIVE);
            buyerHolding.setEntryDate(now);
            buyerHolding.setCreatedAt(now);
            incrementInvestorCount(property);
        } else {
            buyerHolding.setSharesOwned(buyerHolding.getSharesOwned().add(shares));
            buyerHolding.setInvestmentAmount(buyerHolding.getInvestmentAmount().add(gross));
            buyerHolding.setCostBasis(buyerHolding.getCostBasis().add(gross));
            buyerHolding.setCurrentValue(buyerHolding.getSharesOwned().multiply(marketPricePerShare).setScale(2, RoundingMode.HALF_UP));
        }
        buyerHolding.setUpdatedAt(now);
        holdingRepository.save(buyerHolding);

        // Mark listing sold
        listing.setStatus(MarketplaceListingStatus.SOLD);
        listing.setBuyerId(buyerId);
        listing.setSoldAt(now);
        listing.setUpdatedAt(now);
        shareListingRepository.save(listing);

        propertyRepository.save(property);

        notificationService.send(
                buyerId,
                NotificationType.INVESTMENT,
                "Purchase complete",
                "You bought " + shares + " shares in " + property.getName() + " for ₹" + gross);
        String sellerMsg = platformFee.compareTo(BigDecimal.ZERO) > 0
                ? "Your " + shares + " shares in " + property.getName() + " were sold for ₹" + gross
                        + " (₹" + sellerProceeds + " credited after ₹" + platformFee + " platform fee)"
                : "Your " + shares + " shares in " + property.getName() + " were sold for ₹" + gross;
        notificationService.send(
                listing.getSellerId(),
                NotificationType.INVESTMENT,
                "Shares sold",
                sellerMsg);

        return toResponse(listing, property.getName());
    }

    @Override
    @Transactional
    public MarketplaceListingResponse cancelListing(String sellerId, String listingId) {
        ShareListing listing = shareListingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        if (!listing.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("Listing not found");
        }
        if (listing.getStatus() != MarketplaceListingStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active listings can be cancelled. Status: " + listing.getStatus());
        }

        listing.setStatus(MarketplaceListingStatus.CANCELLED);
        listing.setUpdatedAt(Instant.now());
        shareListingRepository.save(listing);

        Property property = propertyRepository.findById(listing.getPropertyId()).orElse(null);
        String propertyName = property != null ? property.getName() : listing.getPropertyId();
        return toResponse(listing, propertyName);
    }

    @Override
    public List<MarketplaceListingResponse> getListingsForProperty(String propertyId) {
        Property property = requireProperty(propertyId);
        return shareListingRepository.findByPropertyIdAndStatus(propertyId, MarketplaceListingStatus.ACTIVE)
                .stream()
                .map(l -> toResponse(l, property.getName()))
                .toList();
    }

    @Override
    public List<MarketplaceListingResponse> getMyListings(String sellerId) {
        return shareListingRepository.findBySellerId(sellerId)
                .stream()
                .map(l -> {
                    Property property = propertyRepository.findById(l.getPropertyId()).orElse(null);
                    String name = property != null ? property.getName() : l.getPropertyId();
                    return toResponse(l, name);
                })
                .toList();
    }

    /**
     * Runs hourly: marks any ACTIVE listing whose {@code expiresAt} has passed as EXPIRED.
     * Provides a stable async sweep on top of the lazy-expire check in {@link #buyListing}.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireStaleListings() {
        Instant now = Instant.now();
        List<ShareListing> stale = shareListingRepository.findByStatus(MarketplaceListingStatus.ACTIVE)
                .stream()
                .filter(l -> l.getExpiresAt() != null && now.isAfter(l.getExpiresAt()))
                .toList();
        for (ShareListing listing : stale) {
            listing.setStatus(MarketplaceListingStatus.EXPIRED);
            listing.setExpiredAt(now);
            listing.setUpdatedAt(now);
            shareListingRepository.save(listing);
            notificationService.send(
                    listing.getSellerId(),
                    NotificationType.INVESTMENT,
                    "Listing expired",
                    "Your listing of " + listing.getSharesListed() + " shares at ₹"
                            + listing.getPricePerShare() + " has expired without a buyer. You can re-list any time.");
        }
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Property requireProperty(String propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
    }

    /**
     * Lock-in: seller cannot list shares until they've held them for at least {@code property.holdPeriodMonths}.
     * If the property has no hold period configured, no check is enforced.
     */
    private void enforceMinHoldPeriod(Property property, PropertyHolding holding) {
        Integer months = property.getHoldPeriodMonths();
        if (months == null || months <= 0) {
            return;
        }
        Instant entry = holding.getEntryDate() != null ? holding.getEntryDate() : holding.getCreatedAt();
        if (entry == null) {
            return;
        }
        Instant unlocksAt = entry.atZone(ZoneOffset.UTC).plusMonths(months).toInstant();
        if (Instant.now().isBefore(unlocksAt)) {
            throw new IllegalArgumentException(
                    "Shares are locked until " + unlocksAt + " (" + months + "-month hold period from purchase).");
        }
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

    private MarketplaceListingResponse toResponse(ShareListing listing, String propertyName) {
        Property property = propertyRepository.findById(listing.getPropertyId()).orElse(null);
        BigDecimal marketPrice = property != null
                ? sharePriceValuationService.getEstimatedSharePrice(property)
                : listing.getPricePerShare();
        BigDecimal discount = marketPrice.subtract(listing.getPricePerShare()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal feePercent = property != null ? property.getMarketplaceFeePercent() : BigDecimal.ZERO;
        BigDecimal gross = listing.getTotalAskPrice();
        BigDecimal platformFee = gross
                .multiply(feePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal sellerProceeds = gross.subtract(platformFee).max(BigDecimal.ZERO);
        return new MarketplaceListingResponse(
                listing.getId(),
                listing.getSellerId(),
                listing.getPropertyId(),
                propertyName,
                listing.getSharesListed(),
                listing.getPricePerShare(),
                gross,
                marketPrice,
                discount,
                feePercent,
                platformFee,
                sellerProceeds,
                listing.getStatus(),
                listing.getCreatedAt(),
                listing.getUpdatedAt(),
                listing.getExpiresAt(),
                listing.getExpiredAt(),
                listing.getSoldAt(),
                listing.getBuyerId());
    }
}
