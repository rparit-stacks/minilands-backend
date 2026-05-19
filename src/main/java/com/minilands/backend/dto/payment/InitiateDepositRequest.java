package com.minilands.backend.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InitiateDepositRequest(
        @NotNull @DecimalMin("1.00") BigDecimal amount
) {
}
