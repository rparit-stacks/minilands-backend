package com.minilands.backend.dto.voting;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Admin records the total proceeds from an off-flow property sale, and the system distributes
 * pro-rata to every active holding ({@code totalSaleProceeds × sharesOwned / totalShares}).
 * The remainder corresponding to unsold/SPV shares is retained by the platform (not credited).
 */
public record DistributeExitProceedsRequest(
        @NotNull @DecimalMin("0.01") BigDecimal totalSaleProceeds,
        String note
) {
}
