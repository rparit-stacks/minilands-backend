package com.minilands.backend.service.voting.impl;

import com.minilands.backend.dto.voting.DistributeExitProceedsRequest;
import com.minilands.backend.dto.voting.ProposalResponse;
import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.PropertyHolding;
import com.minilands.backend.entity.PropertySaleProposal;
import com.minilands.backend.entity.Wallet;
import com.minilands.backend.entity.enums.HoldingStatus;
import com.minilands.backend.entity.enums.NotificationType;
import com.minilands.backend.entity.enums.ProposalStatus;
import com.minilands.backend.entity.enums.PropertyStatus;
import com.minilands.backend.entity.enums.TransactionType;
import com.minilands.backend.repository.PropertyHoldingRepository;
import com.minilands.backend.repository.PropertyRepository;
import com.minilands.backend.repository.PropertySaleProposalRepository;
import com.minilands.backend.service.notification.NotificationService;
import com.minilands.backend.service.voting.AdminPropertySaleService;
import com.minilands.backend.service.wallet.WalletLedgerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
public class AdminPropertySaleServiceImpl implements AdminPropertySaleService {

    private final PropertySaleProposalRepository proposalRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyHoldingRepository holdingRepository;
    private final NotificationService notificationService;
    private final WalletLedgerService walletLedgerService;

    public AdminPropertySaleServiceImpl(
            PropertySaleProposalRepository proposalRepository,
            PropertyRepository propertyRepository,
            PropertyHoldingRepository holdingRepository,
            NotificationService notificationService,
            WalletLedgerService walletLedgerService) {
        this.proposalRepository = proposalRepository;
        this.propertyRepository = propertyRepository;
        this.holdingRepository = holdingRepository;
        this.notificationService = notificationService;
        this.walletLedgerService = walletLedgerService;
    }

    @Override
    public List<ProposalResponse> listPendingApproval() {
        return proposalRepository.findByStatus(ProposalStatus.PENDING_ADMIN_APPROVAL)
                .stream()
                .map(this::toProposalResponse)
                .toList();
    }

    @Override
    public List<ProposalResponse> listAwaitingDistribution() {
        return proposalRepository.findByStatus(ProposalStatus.APPROVED)
                .stream()
                .map(this::toProposalResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProposalResponse approveProposal(String adminId, String proposalId, String note) {
        PropertySaleProposal proposal = requirePendingProposal(proposalId);

        Instant now = Instant.now();
        proposal.setStatus(ProposalStatus.APPROVED);
        proposal.setReviewedByAdminId(adminId);
        proposal.setAdminNote(note);
        proposal.setReviewedAt(now);
        proposalRepository.save(proposal);

        Property property = propertyRepository.findById(proposal.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
        property.setStatus(PropertyStatus.SOLD);
        property.setUpdatedAt(now);
        propertyRepository.save(property);

        List<PropertyHolding> holdings = holdingRepository.findByPropertyIdAndStatus(
                proposal.getPropertyId(), HoldingStatus.ACTIVE);
        for (PropertyHolding holding : holdings) {
            notificationService.send(
                    holding.getUserId(),
                    NotificationType.EXIT,
                    "Property sale approved — exit now available",
                    "Admin has approved the sale of " + property.getName() + ". You can now exit your holding and receive your full payout (share value + all accumulated ROI)."
                            + (note != null && !note.isBlank() ? " Note: " + note : ""));
        }

        return toProposalResponse(proposal);
    }

    @Override
    @Transactional
    public ProposalResponse rejectProposal(String adminId, String proposalId, String note) {
        PropertySaleProposal proposal = requirePendingProposal(proposalId);

        Instant now = Instant.now();
        proposal.setStatus(ProposalStatus.REJECTED);
        proposal.setReviewedByAdminId(adminId);
        proposal.setAdminNote(note);
        proposal.setReviewedAt(now);
        proposalRepository.save(proposal);

        Property property = propertyRepository.findById(proposal.getPropertyId()).orElse(null);
        String propertyName = property != null ? property.getName() : proposal.getPropertyId();

        List<PropertyHolding> holdings = holdingRepository.findByPropertyIdAndStatus(
                proposal.getPropertyId(), HoldingStatus.ACTIVE);
        for (PropertyHolding holding : holdings) {
            notificationService.send(
                    holding.getUserId(),
                    NotificationType.VOTE,
                    "Property sale rejected by admin",
                    "Admin has rejected the sale proposal for " + propertyName + ". The property remains active."
                            + (note != null && !note.isBlank() ? " Reason: " + note : ""));
        }

        return toProposalResponse(proposal);
    }

    @Override
    @Transactional
    public ProposalResponse distributeProceeds(
            String adminId, String proposalId, DistributeExitProceedsRequest request) {
        PropertySaleProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found"));
        if (proposal.getStatus() != ProposalStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "Proceeds can only be distributed for APPROVED proposals. Current status: " + proposal.getStatus());
        }

        Property property = propertyRepository.findById(proposal.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
        if (property.getTotalShares() == null || property.getTotalShares() <= 0) {
            throw new IllegalStateException("Property totalShares must be positive to distribute proceeds.");
        }

        BigDecimal totalSaleProceeds = request.totalSaleProceeds();
        BigDecimal totalSharesBd = BigDecimal.valueOf(property.getTotalShares());

        List<PropertyHolding> holdings = holdingRepository.findByPropertyIdAndStatus(
                proposal.getPropertyId(), HoldingStatus.ACTIVE);

        Instant now = Instant.now();
        BigDecimal investorTotal = BigDecimal.ZERO;

        // Pro-rata: each holding receives totalSaleProceeds × sharesOwned / totalShares.
        // Unsold-share portion is implicitly retained by the platform (SPV).
        for (PropertyHolding holding : holdings) {
            BigDecimal shares = holding.getSharesOwned() != null ? holding.getSharesOwned() : BigDecimal.ZERO;
            if (shares.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal shareProceeds = totalSaleProceeds
                    .multiply(shares)
                    .divide(totalSharesBd, 2, RoundingMode.HALF_UP);

            Wallet wallet = walletLedgerService.requireWallet(holding.getUserId());
            walletLedgerService.credit(
                    wallet,
                    shareProceeds,
                    TransactionType.EXIT_PROCEEDS,
                    "Exit proceeds: " + property.getName() + " (" + shares + " shares)",
                    property.getId());

            holding.setStatus(HoldingStatus.WITHDRAWN);
            holding.setWithdrawnAt(now);
            holding.setCurrentValue(BigDecimal.ZERO);
            holding.setSharesOwned(BigDecimal.ZERO);
            holding.setUpdatedAt(now);
            holdingRepository.save(holding);

            investorTotal = investorTotal.add(shareProceeds);

            notificationService.send(
                    holding.getUserId(),
                    NotificationType.EXIT,
                    "Exit proceeds received",
                    "₹" + shareProceeds + " credited to your wallet from the sale of " + property.getName()
                            + " (" + shares + " shares).");
        }

        BigDecimal spvProceeds = totalSaleProceeds.subtract(investorTotal).max(BigDecimal.ZERO);

        proposal.setStatus(ProposalStatus.DISTRIBUTED);
        proposal.setTotalSaleProceeds(totalSaleProceeds);
        proposal.setInvestorProceedsTotal(investorTotal);
        proposal.setSpvProceeds(spvProceeds);
        proposal.setDistributedByAdminId(adminId);
        proposal.setDistributedAt(now);
        if (request.note() != null && !request.note().isBlank()) {
            proposal.setAdminNote(request.note());
        }
        proposalRepository.save(proposal);

        property.setStatus(PropertyStatus.EXITED);
        property.setCurrentInvestors(0);
        property.setUpdatedAt(now);
        propertyRepository.save(property);

        return toProposalResponse(proposal);
    }

    private PropertySaleProposal requirePendingProposal(String proposalId) {
        PropertySaleProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found"));
        if (proposal.getStatus() != ProposalStatus.PENDING_ADMIN_APPROVAL) {
            throw new IllegalArgumentException(
                    "Proposal is not awaiting admin approval. Current status: " + proposal.getStatus());
        }
        return proposal;
    }

    private ProposalResponse toProposalResponse(PropertySaleProposal proposal) {
        return new ProposalResponse(
                proposal.getId(),
                proposal.getPropertyId(),
                proposal.getStatus(),
                proposal.getThresholdReachedAt(),
                proposal.getReviewedByAdminId(),
                proposal.getAdminNote(),
                proposal.getReviewedAt(),
                proposal.getTotalSaleProceeds(),
                proposal.getInvestorProceedsTotal(),
                proposal.getSpvProceeds(),
                proposal.getDistributedByAdminId(),
                proposal.getDistributedAt());
    }
}
