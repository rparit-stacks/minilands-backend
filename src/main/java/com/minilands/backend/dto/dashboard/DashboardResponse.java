package com.minilands.backend.dto.dashboard;

import com.minilands.backend.dto.investment.HoldingDetailResponse;
import com.minilands.backend.dto.voting.ProposalResponse;
import com.minilands.backend.dto.wallet.TransactionResponse;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(

        // ── TEXT SECTION ─────────────────────────────────────────────────────────
        Summary summary,
        BigDecimal walletBalance,
        List<HoldingDetailResponse> holdings,
        List<ProposalResponse> pendingVotes,
        List<TransactionResponse> recentTransactions,

        // ── CHART DATA ───────────────────────────────────────────────────────────
        Charts charts

) {

    // ── TEXT RECORDS ─────────────────────────────────────────────────────────────

    public record Summary(
            BigDecimal totalInvested,
            BigDecimal totalCurrentValue,
            BigDecimal totalRoiEarned,
            BigDecimal unrealizedProfitOrLoss,
            BigDecimal roiPercentage,
            int activeHoldings,

            // human-readable labels frontend can render directly
            String totalInvestedLabel,         // e.g. "₹1,50,000"
            String totalCurrentValueLabel,     // e.g. "₹1,59,228"
            String totalRoiEarnedLabel,        // e.g. "₹9,228"
            String unrealizedPnlLabel,         // e.g. "+₹9,228 (6.15%)"
            String roiPercentageLabel,         // e.g. "6.15%"
            String walletBalanceLabel          // e.g. "₹25,000"
    ) {
    }

    // ── CHART RECORDS ─────────────────────────────────────────────────────────────

    public record Charts(
            List<PieSlice> portfolioAllocation,   // pie: value split across properties
            List<PieSlice> portfolioComposition,  // pie: invested vs ROI vs wallet
            List<MonthlyDataPoint> roiTimeline    // line/bar: monthly ROI for last 6 months
    ) {
    }

    /**
     * One slice of a pie chart.
     * Frontend uses: label (text), value (raw), percentage (display), color (optional hint).
     */
    public record PieSlice(
            String label,
            BigDecimal value,
            BigDecimal percentage,
            String color
    ) {
    }

    /**
     * One point on the monthly ROI timeline (line chart / bar chart).
     * Frontend uses: month as x-axis label, roiEarned as y-axis value.
     */
    public record MonthlyDataPoint(
            String month,          // e.g. "Dec 2024", "Jan 2025"
            BigDecimal roiEarned
    ) {
    }
}
