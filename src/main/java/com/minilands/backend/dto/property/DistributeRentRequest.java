package com.minilands.backend.dto.property;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record DistributeRentRequest(
        /** Overrides property.monthlyRent when set. */
        @DecimalMin("0.01") BigDecimal monthlyRent,
        @Min(0) @Max(100) Integer platformFeePercent
) {
}
