package com.minilands.backend.dto.marketplace;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ListSharesRequest(
        @NotBlank String propertyId,
        @NotNull @DecimalMin("0.0001") BigDecimal shares,
        @NotNull @DecimalMin(value = "1.00", message = "Ask price per share must be at least ₹1") BigDecimal askPricePerShare
) {
}
