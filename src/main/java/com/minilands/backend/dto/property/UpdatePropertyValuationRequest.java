package com.minilands.backend.dto.property;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdatePropertyValuationRequest(
        @NotNull @DecimalMin("0.01") BigDecimal totalValue,
        @Size(max = 500) String note
) {
}
