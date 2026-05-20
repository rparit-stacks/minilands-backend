package com.minilands.backend.service.voting;

import com.minilands.backend.dto.voting.SaleVoteStatusResponse;

/**
 * Investors opt in/out of selling a property at any time.
 * When opt-in % crosses the admin-configured threshold, admins are notified for final approval.
 */
public interface PropertyVotingService {

    SaleVoteStatusResponse optIn(String userId, String propertyId);

    SaleVoteStatusResponse optOut(String userId, String propertyId);

    SaleVoteStatusResponse getStatus(String userId, String propertyId);
}
