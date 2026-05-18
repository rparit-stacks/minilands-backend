package com.minilands.backend.dto.voting;

import com.minilands.backend.entity.enums.VoteChoice;

import java.time.Instant;

public record VoteResponse(
        String id,
        String proposalId,
        String investorId,
        VoteChoice vote,
        Instant votedAt
) {
}
