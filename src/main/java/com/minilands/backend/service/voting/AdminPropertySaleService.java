package com.minilands.backend.service.voting;

import com.minilands.backend.dto.voting.ProposalResponse;

import java.util.List;

/**
 * Admin-side: review proposals that have passed the 70% investor vote and decide final outcome.
 */
public interface AdminPropertySaleService {

    List<ProposalResponse> listPendingApproval();

    ProposalResponse approveProposal(String adminId, String proposalId, String note);

    ProposalResponse rejectProposal(String adminId, String proposalId, String note);
}
