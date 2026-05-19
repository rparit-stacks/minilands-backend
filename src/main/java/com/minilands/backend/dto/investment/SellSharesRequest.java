package com.minilands.backend.dto.investment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SellSharesRequest(
        @NotBlank String propertyId,
        @NotNull @DecimalMin("0.0001") BigDecimal shares
) {
}
