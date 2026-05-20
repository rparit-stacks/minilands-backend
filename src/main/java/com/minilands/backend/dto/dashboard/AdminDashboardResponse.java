package com.minilands.backend.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(

        // ── PLATFORM OVERVIEW ────────────────────────────────────────────────────
        PlatformSummary summary,

        // ── PENDING ACTION COUNTS ────────────────────────────────────────────────
        ActionQueue actionQueue,

        // ── CHART DATA ───────────────────────────────────────────────────────────
        Charts charts

) {

    /**
     * Top-level platform-wide KPIs shown as stat cards.
     */
    public record PlatformSummary(
            long totalUsers,
            long activeUsers,
            long totalProperties,
            long activeProperties,
            long soldProperties,
            BigDecimal totalFundsRaised,         // sum of totalRaised across all properties
            BigDecimal totalRoiDistributed,      // sum of all RoiDistribution.totalDistributed
            long totalActiveHoldings,
            long activeMarketplaceListings,
            long activeSaleVotes,                // properties with saleVotePercent > 0

            // human-readable labels
            String totalFundsRaisedLabel,        // e.g. "₹2,34,56,000"
            String totalRoiDistributedLabel      // e.g. "₹14,50,000"
    ) {
    }

    /**
     * Counts of items waiting for admin action.
     */
    public record ActionQueue(
            long pendingKyc,
            long pendingWithdrawals,
            long pendingSaleProposals
    ) {
    }

    /**
     * All chart datasets for the admin dashboard.
     */
    public record Charts(
            List<PieSlice> propertiesByStatus,         // pie: property count per status
            List<PieSlice> usersByKycStatus,           // pie: users by KYC approval status
            List<MonthlyBarPoint> monthlyFundsRaised,  // bar: totalRaised added each month (last 12 months)
            List<MonthlyBarPoint> monthlyRoiPaid,      // bar: ROI distributed each month (last 12 months)
            List<MonthlyBarPoint> monthlyNewUsers,     // bar: new user signups per month (last 12 months)
            List<SaleVoteItem> activeSaleVoteDetails   // table/list: properties with active votes
    ) {
    }

    /**
     * One slice of a pie / doughnut chart.
     */
    public record PieSlice(
            String label,
            long count,
            BigDecimal value,       // optional — monetary value when relevant, else null
            String color
    ) {
    }

    /**
     * One bar in a bar / line chart grouped by month.
     */
    public record MonthlyBarPoint(
            String month,           // e.g. "Jun 2025"
            BigDecimal value,       // monetary amount or count as BigDecimal
            long count              // raw count (0 when not applicable)
    ) {
    }

    /**
     * Row for the active sale votes table.
     */
    public record SaleVoteItem(
            String propertyId,
            String propertyName,
            BigDecimal votePercent,
            BigDecimal thresholdPercent,
            int optedInCount,
            int totalInvestors,
            String proposalStatus   // null / "ACTIVE" / "PENDING_ADMIN_APPROVAL" etc.
    ) {
    }
}
