package com.minilands.backend.dto.property;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record MonthlyPaymentExecuteRequest(
        /** Overrides {@code Property.monthlyRent} when set. */
        @DecimalMin(value = "0.01", inclusive = true) BigDecimal monthlyAmountOverride,
        @Min(0) @Max(100) Integer platformFeePercentOverride
) {
}
