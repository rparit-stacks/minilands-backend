package com.minilands.backend.dto.voting;

import com.minilands.backend.entity.enums.ProposalStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ProposalResponse(
        String id,
        String propertyId,
        ProposalStatus status,
        Instant thresholdReachedAt,
        String reviewedByAdminId,
        String adminNote,
        Instant reviewedAt,
        BigDecimal totalSaleProceeds,
        BigDecimal investorProceedsTotal,
        BigDecimal spvProceeds,
        String distributedByAdminId,
        Instant distributedAt
) {
}
