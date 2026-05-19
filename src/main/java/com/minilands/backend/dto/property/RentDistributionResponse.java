package com.minilands.backend.dto.property;

import java.math.BigDecimal;

public record RentDistributionResponse(
        String propertyId,
        BigDecimal monthlyRent,
        BigDecimal platformFee,
        BigDecimal distributableRent,
        int investorsPaid,
        BigDecimal totalDistributed
) {
}
