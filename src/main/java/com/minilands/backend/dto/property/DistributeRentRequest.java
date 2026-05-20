package com.minilands.backend.dto.property;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record DistributeRentRequest(
        /** Overrides property.monthlyRent when set. */
        @DecimalMin("0.01") BigDecimal monthlyRent,
        @Min(0) @Max(100) Integer platformFeePercent,
        /** Target year for distribution; defaults to current year when null. */
        Integer year,
        /** Target month (1-12) for distribution; defaults to current month when null. */
        @Min(1) @Max(12) Integer month
) {
}
