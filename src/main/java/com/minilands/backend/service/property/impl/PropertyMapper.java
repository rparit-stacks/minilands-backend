package com.minilands.backend.service.property.impl;

import com.minilands.backend.dto.property.PropertyDetailResponse;
import com.minilands.backend.dto.property.PropertyDocumentDto;
import com.minilands.backend.dto.property.PropertyLocationDto;
import com.minilands.backend.dto.property.PropertyMediaResponse;
import com.minilands.backend.dto.property.PropertySummaryResponse;
import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.PropertyMedia;
import com.minilands.backend.entity.embeddable.PropertyDocumentRef;
import com.minilands.backend.entity.embeddable.PropertyLocation;
import com.minilands.backend.entity.enums.PropertyStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class PropertyMapper {

    public PropertySummaryResponse toSummary(Property property, String primaryImageUrl) {
        return new PropertySummaryResponse(
                property.getId(),
                property.getSlug(),
                property.getName(),
                property.getTagline(),
                property.getShortDescription(),
                property.getPropertyType(),
                locationCity(property),
                locationState(property),
                locationCountry(property),
                property.getLocationDisplay(),
                primaryImageUrl,
                property.getCurrency(),
                property.getTotalTarget(),
                property.getTotalShares(),
                property.getSharePrice(),
                property.getCurrentPrice(),
                property.getMinInvestmentAmount(),
                property.getAnnualRoi(),
                property.getMonthlyRoi(),
                property.getProjectedAnnualYield(),
                property.getStatus(),
                property.isFeatured(),
                property.getCurrentInvestors(),
                property.getTotalRaised(),
                fundingProgressPercent(property),
                sharesRemaining(property),
                isFundingOpen(property),
                property.getFundingDeadline(),
                property.getPublishedAt());
    }

    public PropertyDetailResponse toDetail(Property property, List<PropertyMedia> media) {
        return new PropertyDetailResponse(
                property.getId(),
                property.getSlug(),
                property.getName(),
                property.getTagline(),
                property.getShortDescription(),
                property.getDescription(),
                property.getPropertyType(),
                property.getAssetClass(),
                toLocationDto(property.getLocation()),
                property.getCurrency(),
                property.getTotalTarget(),
                property.getTotalShares(),
                property.getSharePrice(),
                property.getCurrentPrice(),
                property.getMinInvestmentAmount(),
                property.getMaxSharesPerInvestor(),
                property.getSharesSold(),
                sharesRemaining(property),
                property.getAnnualRoi(),
                property.getMonthlyRoi(),
                property.getProjectedAnnualYield(),
                property.getRentalYieldPercent(),
                property.getExpectedIrrPercent(),
                property.getAppreciationRatePercent(),
                property.getMonthlyRent(),
                property.getRentPlatformFeePercent(),
                property.getDistributionFrequency(),
                property.getHoldPeriodMonths(),
                property.getDeveloperName(),
                property.getBuilderName(),
                property.getSpvName(),
                property.getReraRegistrationId(),
                property.getYearBuilt(),
                property.getCarpetAreaSqFt(),
                property.getBuiltUpAreaSqFt(),
                property.getPlotAreaSqFt(),
                property.getFloors(),
                property.getTotalUnits(),
                property.getAmenities(),
                property.getHighlights(),
                property.getLegalStatus(),
                property.isTitleVerified(),
                property.getDueDiligenceSummary(),
                property.getDocuments().stream().map(this::toDocumentDto).toList(),
                property.getStatus(),
                property.getListingVisibility(),
                property.isFeatured(),
                property.getDisplayOrder(),
                property.getCurrentInvestors(),
                property.getTotalRaised(),
                fundingProgressPercent(property),
                isFundingOpen(property),
                property.getSaleThresholdPercent(),
                property.getSaleVotePercent(),
                property.getSaleVoteOptInCount(),
                property.getFundingDeadline(),
                property.getFundedAt(),
                property.getPublishedAt(),
                property.getCreatedAt(),
                property.getUpdatedAt(),
                media.stream().map(this::toMediaResponse).toList());
    }

    public PropertyLocation toLocationEntity(PropertyLocationDto dto) {
        if (dto == null) {
            return null;
        }
        PropertyLocation location = new PropertyLocation();
        location.setAddressLine1(dto.addressLine1());
        location.setAddressLine2(dto.addressLine2());
        location.setLocality(dto.locality());
        location.setLandmark(dto.landmark());
        location.setCity(dto.city());
        location.setState(dto.state());
        location.setCountry(dto.country());
        location.setPostalCode(dto.postalCode());
        location.setLatitude(dto.latitude());
        location.setLongitude(dto.longitude());
        return location;
    }

    public PropertyLocationDto toLocationDto(PropertyLocation location) {
        if (location == null) {
            return null;
        }
        return new PropertyLocationDto(
                location.getAddressLine1(),
                location.getAddressLine2(),
                location.getLocality(),
                location.getLandmark(),
                location.getCity(),
                location.getState(),
                location.getCountry(),
                location.getPostalCode(),
                location.getLatitude(),
                location.getLongitude());
    }

    public PropertyDocumentRef toDocumentEntity(PropertyDocumentDto dto) {
        PropertyDocumentRef ref = new PropertyDocumentRef();
        ref.setTitle(dto.title());
        ref.setDocumentUrl(dto.documentUrl());
        ref.setDocumentType(dto.documentType());
        return ref;
    }

    public PropertyDocumentDto toDocumentDto(PropertyDocumentRef ref) {
        return new PropertyDocumentDto(ref.getTitle(), ref.getDocumentUrl(), ref.getDocumentType());
    }

    public PropertyMediaResponse toMediaResponse(PropertyMedia media) {
        return new PropertyMediaResponse(
                media.getId(),
                media.getMediaType(),
                media.getMediaUrl(),
                media.isPrimary(),
                media.getDisplayOrder(),
                media.getCaption());
    }

    public String resolvePrimaryImageUrl(List<PropertyMedia> media) {
        return media.stream()
                .filter(PropertyMedia::isPrimary)
                .map(PropertyMedia::getMediaUrl)
                .findFirst()
                .orElseGet(() -> media.stream()
                        .map(PropertyMedia::getMediaUrl)
                        .findFirst()
                        .orElse(null));
    }

    public BigDecimal fundingProgressPercent(Property property) {
        if (property.getTotalTarget() == null
                || property.getTotalTarget().compareTo(BigDecimal.ZERO) <= 0
                || property.getTotalRaised() == null) {
            return BigDecimal.ZERO;
        }
        return property.getTotalRaised()
                .multiply(BigDecimal.valueOf(100))
                .divide(property.getTotalTarget(), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal sharesRemaining(Property property) {
        if (property.getTotalShares() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal sold = property.getSharesSold() != null ? property.getSharesSold() : BigDecimal.ZERO;
        return BigDecimal.valueOf(property.getTotalShares()).subtract(sold).max(BigDecimal.ZERO);
    }

    public boolean isFundingOpen(Property property) {
        return property.getStatus() == PropertyStatus.OPEN || property.getStatus() == PropertyStatus.COMING_SOON;
    }

    private String locationCity(Property property) {
        return property.getLocation() != null ? property.getLocation().getCity() : null;
    }

    private String locationState(Property property) {
        return property.getLocation() != null ? property.getLocation().getState() : null;
    }

    private String locationCountry(Property property) {
        return property.getLocation() != null ? property.getLocation().getCountry() : null;
    }
}
