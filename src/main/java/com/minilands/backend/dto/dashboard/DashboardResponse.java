package com.minilands.backend.dto.dashboard;

import com.minilands.backend.dto.investment.HoldingResponse;
import com.minilands.backend.dto.voting.ProposalResponse;
import com.minilands.backend.dto.wallet.TransactionResponse;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        PortfolioSummary portfolioSummary,
        BigDecimal walletBalance,
        List<HoldingResponse> holdings,
        List<ProposalResponse> pendingVotes,
        List<UpcomingRoi> upcomingRoi,
        List<TransactionResponse> recentTransactions
) {
    public record PortfolioSummary(
            BigDecimal totalInvested,
            BigDecimal totalCurrentValue,
            BigDecimal totalRoiEarned,
            BigDecimal roiPercentage,
            int activeHoldings
    ) {
    }

    public record UpcomingRoi(
            String propertyId,
            String propertyName,
            BigDecimal expectedAmount,
            String expectedDate
    ) {
    }
}
