package com.minilands.backend.dto.voting;

import com.minilands.backend.entity.enums.ProposalStatus;

import java.time.Instant;

public record ProposalResponse(
        String id,
        String propertyId,
        String initiatedBy,
        ProposalStatus status,
        Instant votingStartDate,
        Instant votingEndDate,
        Integer totalVotesNeeded,
        Integer votesReceived,
        Integer votesYes,
        Integer votesNo
) {
}
