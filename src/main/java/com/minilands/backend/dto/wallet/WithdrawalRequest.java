package com.minilands.backend.dto.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WithdrawalRequest(
        @NotNull @DecimalMin("1.00") BigDecimal amount,
        @NotBlank String bankAccountId
) {
}
