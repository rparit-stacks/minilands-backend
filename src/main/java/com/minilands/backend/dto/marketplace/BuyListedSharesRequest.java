package com.minilands.backend.dto.marketplace;

import jakarta.validation.constraints.NotBlank;

public record BuyListedSharesRequest(
        @NotBlank String listingId
) {
}
