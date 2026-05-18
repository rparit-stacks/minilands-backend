package com.minilands.backend.dto.voting;

import com.minilands.backend.entity.enums.VoteChoice;

public record VoteRequest(
        VoteChoice vote
) {
}
