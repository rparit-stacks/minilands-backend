package com.minilands.backend.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdminWalletAdjustRequest(
        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        @Digits(integer = 12, fraction = 2)
        BigDecimal amount,
        @NotNull AdminWalletAdjustDirection direction,
        @Size(max = 500)
        String note
) {
}
