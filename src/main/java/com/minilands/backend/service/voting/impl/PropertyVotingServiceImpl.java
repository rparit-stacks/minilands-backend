package com.minilands.backend.service.voting.impl;

import com.minilands.backend.dto.voting.SaleVoteStatusResponse;
import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.PropertyHolding;
import com.minilands.backend.entity.PropertySaleProposal;
import com.minilands.backend.entity.PropertyVote;
import com.minilands.backend.entity.enums.AccountStatus;
import com.minilands.backend.entity.enums.HoldingStatus;
import com.minilands.backend.entity.enums.NotificationType;
import com.minilands.backend.entity.enums.ProposalStatus;
import com.minilands.backend.entity.enums.PropertyStatus;
import com.minilands.backend.repository.AdminRepository;
import com.minilands.backend.repository.PropertyHoldingRepository;
import com.minilands.backend.repository.PropertyRepository;
import com.minilands.backend.repository.PropertySaleProposalRepository;
import com.minilands.backend.repository.PropertyVoteRepository;
import com.minilands.backend.repository.UserRepository;
import com.minilands.backend.service.notification.NotificationService;
import com.minilands.backend.service.voting.PropertyVotingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class PropertyVotingServiceImpl implements PropertyVotingService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyHoldingRepository holdingRepository;
    private final PropertySaleProposalRepository proposalRepository;
    private final PropertyVoteRepository voteRepository;
    private final NotificationService notificationService;

    public PropertyVotingServiceImpl(
            UserRepository userRepository,
            AdminRepository adminRepository,
            PropertyRepository propertyRepository,
            PropertyHoldingRepository holdingRepository,
            PropertySaleProposalRepository proposalRepository,
            PropertyVoteRepository voteRepository,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.propertyRepository = propertyRepository;
        this.holdingRepository = holdingRepository;
        this.proposalRepository = proposalRepository;
        this.voteRepository = voteRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public SaleVoteStatusResponse optIn(String userId, String propertyId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Property property = requireProperty(propertyId);
        assertTradableProperty(property);
        assertHasActiveHolding(userId, propertyId);

        // Idempotent — if already opted in, just return current status
        if (voteRepository.findByPropertyIdAndInvestorId(propertyId, userId).isPresent()) {
            return buildStatus(userId, property);
        }

        Instant now = Instant.now();
        PropertyVote vote = new PropertyVote();
        vote.setPropertyId(propertyId);
        vote.setInvestorId(userId);
        vote.setOptedInAt(now);
        voteRepository.save(vote);

        recalculateAndSave(property, now);

        // Check if threshold just crossed — escalate to admin
        checkAndEscalate(property, propertyId);

        return buildStatus(userId, property);
    }

    @Override
    @Transactional
    public SaleVoteStatusResponse optOut(String userId, String propertyId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Property property = requireProperty(propertyId);
        assertHasActiveHolding(userId, propertyId);

        voteRepository.deleteByPropertyIdAndInvestorId(propertyId, userId);
        recalculateAndSave(property, Instant.now());

        // If a PENDING_ADMIN_APPROVAL proposal existed and opt-in dropped below threshold, revert it to ACTIVE
        proposalRepository.findByPropertyId(propertyId).ifPresent(proposal -> {
            if (proposal.getStatus() == ProposalStatus.PENDING_ADMIN_APPROVAL) {
                BigDecimal threshold = property.getSaleThresholdPercent();
                if (property.getSaleVotePercent().compareTo(threshold) < 0) {
                    proposal.setStatus(ProposalStatus.ACTIVE);
                    proposal.setThresholdReachedAt(null);
                    proposalRepository.save(proposal);

                    // Notify investors the threshold dropped
                    List<PropertyHolding> holdings = holdingRepository.findByPropertyIdAndStatus(propertyId, HoldingStatus.ACTIVE);
                    for (PropertyHolding h : holdings) {
                        notificationService.send(h.getUserId(), NotificationType.VOTE,
                                "Sale vote dropped below threshold",
                                property.getName() + " sale vote is now at " + property.getSaleVotePercent() + "% — below the " + threshold + "% required. Admin approval request has been retracted.");
                    }
                }
            }
        });

        return buildStatus(userId, property);
    }

    @Override
    public SaleVoteStatusResponse getStatus(String userId, String propertyId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Property property = requireProperty(propertyId);
        return buildStatus(userId, property);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private void recalculateAndSave(Property property, Instant now) {
        long optInCount = voteRepository.countByPropertyId(property.getId());
        int totalInvestors = property.getCurrentInvestors() != null ? property.getCurrentInvestors() : 0;

        BigDecimal percent = BigDecimal.ZERO;
        if (totalInvestors > 0) {
            percent = BigDecimal.valueOf(optInCount)
                    .divide(BigDecimal.valueOf(totalInvestors), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        property.setSaleVoteOptInCount((int) optInCount);
        property.setSaleVotePercent(percent);
        property.setUpdatedAt(now);
        propertyRepository.save(property);
    }

    private void checkAndEscalate(Property property, String propertyId) {
        BigDecimal threshold = property.getSaleThresholdPercent();
        if (property.getSaleVotePercent().compareTo(threshold) < 0) return;

        // Already escalated?
        Optional<PropertySaleProposal> existing = proposalRepository.findByPropertyId(propertyId);
        if (existing.isPresent()) {
            ProposalStatus s = existing.get().getStatus();
            if (s == ProposalStatus.PENDING_ADMIN_APPROVAL || s == ProposalStatus.APPROVED) return;
        }

        Instant now = Instant.now();
        PropertySaleProposal proposal = existing.orElseGet(() -> {
            PropertySaleProposal p = new PropertySaleProposal();
            p.setPropertyId(propertyId);
            p.setCreatedAt(now);
            return p;
        });
        proposal.setStatus(ProposalStatus.PENDING_ADMIN_APPROVAL);
        proposal.setThresholdReachedAt(now);
        proposalRepository.save(proposal);

        // Notify investors
        List<PropertyHolding> holdings = holdingRepository.findByPropertyIdAndStatus(propertyId, HoldingStatus.ACTIVE);
        for (PropertyHolding h : holdings) {
            notificationService.send(h.getUserId(), NotificationType.VOTE,
                    "Sale threshold reached — awaiting admin approval",
                    property.getName() + " sale vote reached " + property.getSaleVotePercent()
                            + "% (threshold: " + threshold + "%). Admin has been notified for final approval.");
        }

        // Notify all active admins
        adminRepository.findAll().stream()
                .filter(a -> a.getAccountStatus() == AccountStatus.ACTIVE)
                .forEach(a -> notificationService.send(a.getId(), NotificationType.VOTE,
                        "Action required: Property sale threshold reached",
                        "\"" + property.getName() + "\" has " + property.getSaleVotePercent()
                                + "% investors opted in to sell (threshold: " + threshold + "%). Review at /api/admin/proposals."));
    }

    private SaleVoteStatusResponse buildStatus(String userId, Property property) {
        boolean optedIn = voteRepository.findByPropertyIdAndInvestorId(property.getId(), userId).isPresent();
        int totalInvestors = property.getCurrentInvestors() != null ? property.getCurrentInvestors() : 0;

        Optional<PropertySaleProposal> proposal = proposalRepository.findByPropertyId(property.getId());
        ProposalStatus proposalStatus = proposal.map(PropertySaleProposal::getStatus).orElse(null);
        Instant thresholdReachedAt = proposal.map(PropertySaleProposal::getThresholdReachedAt).orElse(null);
        String adminNote = proposal.map(PropertySaleProposal::getAdminNote).orElse(null);
        BigDecimal totalSaleProceeds = proposal.map(PropertySaleProposal::getTotalSaleProceeds).orElse(null);
        BigDecimal investorProceedsTotal = proposal.map(PropertySaleProposal::getInvestorProceedsTotal).orElse(null);
        BigDecimal spvProceeds = proposal.map(PropertySaleProposal::getSpvProceeds).orElse(null);
        Instant distributedAt = proposal.map(PropertySaleProposal::getDistributedAt).orElse(null);

        return new SaleVoteStatusResponse(
                property.getId(),
                property.getName(),
                totalInvestors,
                property.getSaleVoteOptInCount(),
                property.getSaleVotePercent(),
                property.getSaleThresholdPercent(),
                optedIn,
                proposalStatus,
                thresholdReachedAt,
                adminNote,
                totalSaleProceeds,
                investorProceedsTotal,
                spvProceeds,
                distributedAt);
    }

    private Property requireProperty(String propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
    }

    private void assertTradableProperty(Property property) {
        if (property.getStatus() != PropertyStatus.ACTIVE && property.getStatus() != PropertyStatus.OPEN) {
            throw new IllegalArgumentException("Sale votes can only be cast on ACTIVE or OPEN properties");
        }
    }

    private void assertHasActiveHolding(String userId, String propertyId) {
        holdingRepository.findByUserIdAndPropertyId(userId, propertyId)
                .filter(h -> h.getStatus() == HoldingStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("You must be an active investor in this property"));
    }
}
