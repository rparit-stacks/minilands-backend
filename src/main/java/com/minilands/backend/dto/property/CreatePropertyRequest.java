package com.minilands.backend.dto.property;

import com.minilands.backend.entity.enums.AssetClass;
import com.minilands.backend.entity.enums.DistributionFrequency;
import com.minilands.backend.entity.enums.LegalStatus;
import com.minilands.backend.entity.enums.ListingVisibility;
import com.minilands.backend.entity.enums.PropertyStatus;
import com.minilands.backend.entity.enums.PropertyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CreatePropertyRequest(
        @Size(max = 120) String slug,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 300) String tagline,
        @Size(max = 500) String shortDescription,
        @NotBlank @Size(max = 10000) String description,
        @NotNull PropertyType propertyType,
        AssetClass assetClass,
        @NotNull @Valid PropertyLocationDto location,
        @Size(max = 3) String currency,
        @NotNull @DecimalMin("0.01") BigDecimal totalTarget,
        @NotNull @Min(1) Integer totalShares,
        /** Optional — computed as totalTarget ÷ totalShares when omitted. */
        @DecimalMin("0.01") BigDecimal sharePrice,
        @DecimalMin("0.01") BigDecimal minInvestmentAmount,
        @Min(1) Integer maxSharesPerInvestor,
        @DecimalMin("0") BigDecimal annualRoi,
        @DecimalMin("0") BigDecimal monthlyRoi,
        @DecimalMin("0") BigDecimal projectedAnnualYield,
        @DecimalMin("0") BigDecimal rentalYieldPercent,
        @DecimalMin("0") BigDecimal expectedIrrPercent,
        @DecimalMin("0") BigDecimal appreciationRatePercent,
        DistributionFrequency distributionFrequency,
        @Min(1) Integer holdPeriodMonths,
        @Size(max = 200) String developerName,
        @Size(max = 200) String builderName,
        @Size(max = 200) String spvName,
        @Size(max = 100) String reraRegistrationId,
        @Min(1800) Integer yearBuilt,
        @DecimalMin("0") BigDecimal carpetAreaSqFt,
        @DecimalMin("0") BigDecimal builtUpAreaSqFt,
        @DecimalMin("0") BigDecimal plotAreaSqFt,
        @Min(1) Integer floors,
        @Min(1) Integer totalUnits,
        List<@NotBlank @Size(max = 120) String> amenities,
        List<@NotBlank @Size(max = 300) String> highlights,
        LegalStatus legalStatus,
        Boolean titleVerified,
        @Size(max = 5000) String dueDiligenceSummary,
        List<@Valid PropertyDocumentDto> documents,
        PropertyStatus status,
        ListingVisibility listingVisibility,
        Boolean featured,
        Integer displayOrder,
        Instant fundingDeadline,
        @Valid List<PropertyMediaItemDto> media
) {
}
