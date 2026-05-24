package com.minilands.backend.dto.voting;

import com.minilands.backend.entity.enums.ProposalStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record SaleVoteStatusResponse(
        String propertyId,
        String propertyName,
        int totalInvestors,
        int optedInCount,
        BigDecimal optInPercent,
        BigDecimal thresholdPercent,
        boolean currentUserOptedIn,
        ProposalStatus proposalStatus,    // null if no proposal exists yet
        Instant thresholdReachedAt,
        String adminNote,
        // Populated when proposalStatus = DISTRIBUTED (bulk exit payout has run).
        BigDecimal totalSaleProceeds,
        BigDecimal investorProceedsTotal,
        BigDecimal spvProceeds,
        Instant distributedAt
) {
}
