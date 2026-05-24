package com.minilands.backend.dto.marketplace;

import com.minilands.backend.entity.enums.MarketplaceListingStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketplaceListingResponse(
        String id,
        String sellerId,
        String propertyId,
        String propertyName,
        BigDecimal sharesListed,
        BigDecimal askPricePerShare,      // seller's custom price
        BigDecimal totalAskPrice,
        BigDecimal marketPricePerShare,   // current market valuation for reference
        BigDecimal discountVsMarket,      // marketPrice - askPrice (positive = buyer gets a deal, negative = premium)
        BigDecimal marketplaceFeePercent, // property's resale fee % at time of view
        BigDecimal platformFee,           // gross × fee%
        BigDecimal sellerProceeds,        // gross − platformFee
        MarketplaceListingStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt,
        Instant expiredAt,
        Instant soldAt,
        String buyerId
) {
}
