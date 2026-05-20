package com.minilands.backend.service.exit.impl;

import com.minilands.backend.dto.exit.ExitResponse;
import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.PropertyHolding;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.Wallet;
import com.minilands.backend.entity.enums.HoldingStatus;
import com.minilands.backend.entity.enums.NotificationType;
import com.minilands.backend.entity.enums.ProposalStatus;
import com.minilands.backend.entity.enums.PropertyStatus;
import com.minilands.backend.entity.enums.TransactionType;
import com.minilands.backend.repository.PropertyHoldingRepository;
import com.minilands.backend.repository.PropertyRepository;
import com.minilands.backend.repository.PropertySaleProposalRepository;
import com.minilands.backend.repository.UserRepository;
import com.minilands.backend.service.exit.PropertyExitService;
import com.minilands.backend.service.kyc.KycGuard;
import com.minilands.backend.service.notification.NotificationService;
import com.minilands.backend.service.property.SharePriceValuationService;
import com.minilands.backend.service.wallet.WalletLedgerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Option 2 exit path: called after a sale proposal is APPROVED (≥70% YES votes).
 * Credits the investor's wallet with: sharesOwned × currentSharePrice + all accumulated ROI.
 */
@Service
public class PropertyExitServiceImpl implements PropertyExitService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyHoldingRepository holdingRepository;
    private final PropertySaleProposalRepository proposalRepository;
    private final WalletLedgerService walletLedgerService;
    private final SharePriceValuationService sharePriceValuationService;
    private final NotificationService notificationService;

    public PropertyExitServiceImpl(
            UserRepository userRepository,
            PropertyRepository propertyRepository,
            PropertyHoldingRepository holdingRepository,
            PropertySaleProposalRepository proposalRepository,
            WalletLedgerService walletLedgerService,
            SharePriceValuationService sharePriceValuationService,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.holdingRepository = holdingRepository;
        this.proposalRepository = proposalRepository;
        this.walletLedgerService = walletLedgerService;
        this.sharePriceValuationService = sharePriceValuationService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public ExitResponse exit(String userId, String holdingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        KycGuard.requireApproved(user);

        PropertyHolding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new IllegalArgumentException("Holding not found"));

        if (!holding.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Holding not found");
        }
        if (holding.getStatus() != HoldingStatus.ACTIVE) {
            throw new IllegalArgumentException("Holding is not active. Already exited or withdrawn.");
        }

        Property property = propertyRepository.findById(holding.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        // Exit is only valid once admin has approved the sale (property status = SOLD)
        if (property.getStatus() != PropertyStatus.SOLD) {
            ProposalStatus proposalStatus = proposalRepository
                    .findByPropertyId(holding.getPropertyId())
                    .stream()
                    .map(p -> p.getStatus())
                    .filter(s -> s == ProposalStatus.ACTIVE || s == ProposalStatus.PENDING_ADMIN_APPROVAL)
                    .findFirst()
                    .orElse(null);

            if (proposalStatus == ProposalStatus.PENDING_ADMIN_APPROVAL) {
                throw new IllegalArgumentException(
                        "Sale vote passed but is awaiting admin approval. You will be notified once admin approves.");
            }
            throw new IllegalArgumentException(
                    "Property exit is only available after admin approves the sale. " +
                    "Initiate a vote via POST /api/voting/properties/{propertyId}/initiate, or list shares on the marketplace instead.");
        }

        BigDecimal shares = holding.getSharesOwned() != null ? holding.getSharesOwned() : BigDecimal.ZERO;
        if (shares.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("No shares to exit");
        }

        BigDecimal currentSharePrice = sharePriceValuationService.getEstimatedSharePrice(property);
        BigDecimal shareValue = shares.multiply(currentSharePrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal roiEarned = holding.getRoiEarned() != null ? holding.getRoiEarned() : BigDecimal.ZERO;
        BigDecimal totalPayout = shareValue.add(roiEarned).setScale(2, RoundingMode.HALF_UP);

        Wallet wallet = walletLedgerService.requireWallet(userId);
        walletLedgerService.credit(
                wallet,
                totalPayout,
                TransactionType.EXIT,
                "Exit " + shares + " shares in " + property.getName() + " (share value: ₹" + shareValue + " + ROI: ₹" + roiEarned + ")",
                holdingId);

        // Settle the holding
        Instant now = Instant.now();
        holding.setStatus(HoldingStatus.WITHDRAWN);
        holding.setWithdrawnAt(now);
        holding.setCurrentValue(BigDecimal.ZERO);
        holding.setSharesOwned(BigDecimal.ZERO);
        holding.setUpdatedAt(now);
        holdingRepository.save(holding);

        // Decrement investor count on property
        int count = property.getCurrentInvestors() != null ? property.getCurrentInvestors() : 0;
        property.setCurrentInvestors(Math.max(0, count - 1));
        propertyRepository.save(property);

        // Fetch the transaction ID just created for the response
        String transactionId = findLatestExitTransactionId(userId, holdingId);

        notificationService.send(
                userId,
                NotificationType.EXIT,
                "Exit successful",
                "You received ₹" + totalPayout + " for your " + shares + " shares in " + property.getName() + " (₹" + shareValue + " share value + ₹" + roiEarned + " ROI).");

        return new ExitResponse(holdingId, totalPayout, transactionId);
    }

    private String findLatestExitTransactionId(String userId, String holdingId) {
        // Return holdingId as a stable reference — the actual transaction can be fetched from /api/wallet/transactions
        return holdingId;
    }
}
