package com.minilands.backend.service.voting;

import com.minilands.backend.dto.voting.DistributeExitProceedsRequest;
import com.minilands.backend.dto.voting.ProposalResponse;

import java.util.List;

/**
 * Admin-side: review proposals that have passed the 70% investor vote and decide final outcome.
 */
public interface AdminPropertySaleService {

    List<ProposalResponse> listPendingApproval();

    /** APPROVED proposals where bulk distribute-proceeds has not yet run. */
    List<ProposalResponse> listAwaitingDistribution();

    ProposalResponse approveProposal(String adminId, String proposalId, String note);

    ProposalResponse rejectProposal(String adminId, String proposalId, String note);

    /**
     * Bulk distribute sale proceeds pro-rata across all ACTIVE holdings of the property tied to this proposal.
     * Pre-condition: proposal status must be APPROVED. Idempotent — re-running after DISTRIBUTED is a no-op error.
     */
    ProposalResponse distributeProceeds(String adminId, String proposalId, DistributeExitProceedsRequest request);
}
