package com.minilands.backend.service.property.impl;

import com.minilands.backend.dto.property.CreatePropertyRequest;
import com.minilands.backend.dto.property.PropertyDetailResponse;
import com.minilands.backend.dto.property.PropertyDocumentDto;
import com.minilands.backend.dto.property.PropertyMediaItemDto;
import com.minilands.backend.dto.property.PropertySummaryResponse;
import com.minilands.backend.dto.property.UpdatePropertyRequest;
import com.minilands.backend.dto.property.UpdatePropertyStatusRequest;
import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.PropertyMedia;
import com.minilands.backend.entity.embeddable.PropertyDocumentRef;
import com.minilands.backend.entity.enums.AssetClass;
import com.minilands.backend.entity.enums.ListingVisibility;
import com.minilands.backend.entity.enums.PropertyStatus;
import com.minilands.backend.entity.enums.ValuationFrequency;
import com.minilands.backend.repository.PropertyMediaRepository;
import com.minilands.backend.repository.PropertyRepository;
import com.minilands.backend.service.property.AdminPropertyCatalogService;
import com.minilands.backend.service.property.PropertyInvestmentMath;
import com.minilands.backend.service.property.PropertySlugSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AdminPropertyCatalogServiceImpl implements AdminPropertyCatalogService {

    private final PropertyRepository propertyRepository;
    private final PropertyMediaRepository propertyMediaRepository;
    private final PropertyMapper propertyMapper;
    private final PropertySlugSupport propertySlugSupport;

    public AdminPropertyCatalogServiceImpl(
            PropertyRepository propertyRepository,
            PropertyMediaRepository propertyMediaRepository,
            PropertyMapper propertyMapper,
            PropertySlugSupport propertySlugSupport) {
        this.propertyRepository = propertyRepository;
        this.propertyMediaRepository = propertyMediaRepository;
        this.propertyMapper = propertyMapper;
        this.propertySlugSupport = propertySlugSupport;
    }

    @Override
    @Transactional
    public PropertyDetailResponse create(String adminId, CreatePropertyRequest request) {
        BigDecimal sharePrice = PropertyInvestmentMath.resolveSharePrice(
                request.totalTarget(), request.totalShares(), request.sharePrice());

        Instant now = Instant.now();
        Property property = new Property();
        property.setSlug(propertySlugSupport.resolveUniqueSlug(request.slug(), request.name(), null));
        applyCreateFields(property, request, sharePrice);
        property.setTotalValue(request.totalTarget());
        property.setValuationFrequency(ValuationFrequency.QUARTERLY);
        property.setLastValuationDate(now);
        property.setCurrentPrice(sharePrice);
        property.setSharesSold(BigDecimal.ZERO);
        property.setTotalRaised(BigDecimal.ZERO);
        property.setCurrentInvestors(0);
        property.setStatus(request.status() != null ? request.status() : PropertyStatus.DRAFT);
        property.setListingVisibility(
                request.listingVisibility() != null ? request.listingVisibility() : ListingVisibility.PUBLIC);
        property.setFeatured(request.featured() != null && request.featured());
        property.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        property.setCreatedByAdminId(adminId);
        property.setCreatedAt(now);
        property.setUpdatedAt(now);
        if (isPublishedStatus(property.getStatus())) {
            property.setPublishedAt(now);
        }

        Property saved = propertyRepository.save(property);
        replaceMedia(saved.getId(), request.media(), now);

        return toDetail(saved);
    }

    @Override
    @Transactional
    public PropertyDetailResponse update(String propertyId, UpdatePropertyRequest request) {
        Property property = findProperty(propertyId);

        if (request.slug() != null) {
            property.setSlug(propertySlugSupport.resolveUniqueSlug(request.slug(), property.getName(), propertyId));
        }
        if (request.name() != null) {
            property.setName(request.name());
        }
        if (request.tagline() != null) {
            property.setTagline(request.tagline());
        }
        if (request.shortDescription() != null) {
            property.setShortDescription(request.shortDescription());
        }
        if (request.description() != null) {
            property.setDescription(request.description());
        }
        if (request.propertyType() != null) {
            property.setPropertyType(request.propertyType());
        }
        if (request.assetClass() != null) {
            property.setAssetClass(request.assetClass());
        }
        if (request.location() != null) {
            property.setLocation(propertyMapper.toLocationEntity(request.location()));
        }
        if (request.currency() != null) {
            property.setCurrency(request.currency());
        }
        if (request.totalTarget() != null) {
            property.setTotalTarget(request.totalTarget());
        }
        if (request.totalShares() != null) {
            property.setTotalShares(request.totalShares());
        }
        boolean investmentTermsTouched = request.totalTarget() != null || request.totalShares() != null;
        if (request.sharePrice() != null || investmentTermsTouched) {
            BigDecimal sharePrice = PropertyInvestmentMath.resolveSharePrice(
                    property.getTotalTarget(),
                    property.getTotalShares(),
                    request.sharePrice());
            property.setSharePrice(sharePrice);
        }
        if (request.currentPrice() != null) {
            property.setCurrentPrice(request.currentPrice());
        }
        if (request.minInvestmentAmount() != null) {
            property.setMinInvestmentAmount(request.minInvestmentAmount());
        }
        if (request.maxSharesPerInvestor() != null) {
            property.setMaxSharesPerInvestor(request.maxSharesPerInvestor());
        }
        if (request.annualRoi() != null) {
            property.setAnnualRoi(request.annualRoi());
        }
        if (request.monthlyRoi() != null) {
            property.setMonthlyRoi(request.monthlyRoi());
        }
        if (request.projectedAnnualYield() != null) {
            property.setProjectedAnnualYield(request.projectedAnnualYield());
        }
        if (request.rentalYieldPercent() != null) {
            property.setRentalYieldPercent(request.rentalYieldPercent());
        }
        if (request.expectedIrrPercent() != null) {
            property.setExpectedIrrPercent(request.expectedIrrPercent());
        }
        if (request.appreciationRatePercent() != null) {
            property.setAppreciationRatePercent(request.appreciationRatePercent());
        }
        if (request.monthlyRent() != null) {
            property.setMonthlyRent(request.monthlyRent());
        }
        if (request.rentPlatformFeePercent() != null) {
            property.setRentPlatformFeePercent(request.rentPlatformFeePercent());
        }
        if (request.marketplaceFeePercent() != null) {
            property.setMarketplaceFeePercent(request.marketplaceFeePercent());
        }
        if (request.distributionFrequency() != null) {
            property.setDistributionFrequency(request.distributionFrequency());
        }
        if (request.holdPeriodMonths() != null) {
            property.setHoldPeriodMonths(request.holdPeriodMonths());
        }
        if (request.developerName() != null) {
            property.setDeveloperName(request.developerName());
        }
        if (request.builderName() != null) {
            property.setBuilderName(request.builderName());
        }
        if (request.spvName() != null) {
            property.setSpvName(request.spvName());
        }
        if (request.reraRegistrationId() != null) {
            property.setReraRegistrationId(request.reraRegistrationId());
        }
        if (request.yearBuilt() != null) {
            property.setYearBuilt(request.yearBuilt());
        }
        if (request.carpetAreaSqFt() != null) {
            property.setCarpetAreaSqFt(request.carpetAreaSqFt());
        }
        if (request.builtUpAreaSqFt() != null) {
            property.setBuiltUpAreaSqFt(request.builtUpAreaSqFt());
        }
        if (request.plotAreaSqFt() != null) {
            property.setPlotAreaSqFt(request.plotAreaSqFt());
        }
        if (request.floors() != null) {
            property.setFloors(request.floors());
        }
        if (request.totalUnits() != null) {
            property.setTotalUnits(request.totalUnits());
        }
        if (request.amenities() != null) {
            property.setAmenities(new ArrayList<>(request.amenities()));
        }
        if (request.highlights() != null) {
            property.setHighlights(new ArrayList<>(request.highlights()));
        }
        if (request.legalStatus() != null) {
            property.setLegalStatus(request.legalStatus());
        }
        if (request.titleVerified() != null) {
            property.setTitleVerified(request.titleVerified());
        }
        if (request.dueDiligenceSummary() != null) {
            property.setDueDiligenceSummary(request.dueDiligenceSummary());
        }
        if (request.documents() != null) {
            property.setDocuments(request.documents().stream()
                    .map(propertyMapper::toDocumentEntity)
                    .toList());
        }
        if (request.listingVisibility() != null) {
            property.setListingVisibility(request.listingVisibility());
        }
        if (request.featured() != null) {
            property.setFeatured(request.featured());
        }
        if (request.displayOrder() != null) {
            property.setDisplayOrder(request.displayOrder());
        }
        if (request.fundingDeadline() != null) {
            property.setFundingDeadline(request.fundingDeadline());
        }
        if (request.saleThresholdPercent() != null) {
            property.setSaleThresholdPercent(request.saleThresholdPercent());
        }

        Instant now = Instant.now();
        property.setUpdatedAt(now);
        Property saved = propertyRepository.save(property);

        if (request.media() != null) {
            replaceMedia(saved.getId(), request.media(), now);
        }

        return toDetail(saved);
    }

    @Override
    @Transactional
    public PropertyDetailResponse updateStatus(String propertyId, UpdatePropertyStatusRequest request) {
        Property property = findProperty(propertyId);
        PropertyStatus newStatus = request.status();
        property.setStatus(newStatus);
        Instant now = Instant.now();
        property.setUpdatedAt(now);

        if (newStatus == PropertyStatus.FUNDED && property.getFundedAt() == null) {
            property.setFundedAt(now);
        }
        if (isPublishedStatus(newStatus) && property.getPublishedAt() == null) {
            property.setPublishedAt(now);
        }

        return toDetail(propertyRepository.save(property));
    }

    @Override
    public PropertyDetailResponse getById(String propertyId) {
        return toDetail(findProperty(propertyId));
    }

    @Override
    public List<PropertySummaryResponse> listAll(PropertyStatus status) {
        List<Property> properties = status != null
                ? propertyRepository.findByStatus(status)
                : propertyRepository.findAll();

        return properties.stream()
                .sorted(Comparator
                        .comparing(Property::isFeatured).reversed()
                        .thenComparing(p -> p.getDisplayOrder() != null ? p.getDisplayOrder() : 0)
                        .thenComparing(Property::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toSummary)
                .toList();
    }

    private Property findProperty(String propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
    }

    private void applyCreateFields(Property property, CreatePropertyRequest request, BigDecimal sharePrice) {
        property.setName(request.name());
        property.setTagline(request.tagline());
        property.setShortDescription(request.shortDescription());
        property.setDescription(request.description());
        property.setPropertyType(request.propertyType());
        property.setAssetClass(request.assetClass() != null ? request.assetClass() : AssetClass.REAL_ESTATE);
        property.setLocation(propertyMapper.toLocationEntity(request.location()));
        property.setCurrency(StringUtils.hasText(request.currency()) ? request.currency() : "INR");
        property.setTotalTarget(request.totalTarget());
        property.setTotalShares(request.totalShares());
        property.setSharePrice(sharePrice);
        property.setMinInvestmentAmount(
                request.minInvestmentAmount() != null ? request.minInvestmentAmount() : sharePrice);
        property.setMaxSharesPerInvestor(request.maxSharesPerInvestor());
        property.setAnnualRoi(request.annualRoi());
        property.setMonthlyRoi(request.monthlyRoi());
        property.setProjectedAnnualYield(request.projectedAnnualYield());
        property.setRentalYieldPercent(request.rentalYieldPercent());
        property.setExpectedIrrPercent(request.expectedIrrPercent());
        property.setAppreciationRatePercent(request.appreciationRatePercent());
        property.setMonthlyRent(request.monthlyRent());
        if (request.rentPlatformFeePercent() != null) {
            property.setRentPlatformFeePercent(request.rentPlatformFeePercent());
        }
        property.setMarketplaceFeePercent(request.marketplaceFeePercent());
        property.setDistributionFrequency(request.distributionFrequency());
        property.setHoldPeriodMonths(request.holdPeriodMonths());
        property.setDeveloperName(request.developerName());
        property.setBuilderName(request.builderName());
        property.setSpvName(request.spvName());
        property.setReraRegistrationId(request.reraRegistrationId());
        property.setYearBuilt(request.yearBuilt());
        property.setCarpetAreaSqFt(request.carpetAreaSqFt());
        property.setBuiltUpAreaSqFt(request.builtUpAreaSqFt());
        property.setPlotAreaSqFt(request.plotAreaSqFt());
        property.setFloors(request.floors());
        property.setTotalUnits(request.totalUnits());
        property.setAmenities(request.amenities() != null ? new ArrayList<>(request.amenities()) : new ArrayList<>());
        property.setHighlights(request.highlights() != null ? new ArrayList<>(request.highlights()) : new ArrayList<>());
        property.setLegalStatus(request.legalStatus());
        property.setTitleVerified(request.titleVerified() != null && request.titleVerified());
        property.setDueDiligenceSummary(request.dueDiligenceSummary());
        property.setDocuments(request.documents() != null
                ? request.documents().stream().map(propertyMapper::toDocumentEntity).toList()
                : new ArrayList<>());
        property.setFundingDeadline(request.fundingDeadline());
        if (request.saleThresholdPercent() != null) {
            property.setSaleThresholdPercent(request.saleThresholdPercent());
        }
    }

    private void replaceMedia(String propertyId, List<PropertyMediaItemDto> mediaItems, Instant now) {
        List<PropertyMedia> existing = propertyMediaRepository.findByPropertyIdOrderByDisplayOrderAsc(propertyId);
        propertyMediaRepository.deleteAll(existing);

        if (mediaItems == null || mediaItems.isEmpty()) {
            return;
        }

        boolean primaryAssigned = false;
        int order = 0;
        for (PropertyMediaItemDto item : mediaItems) {
            PropertyMedia media = new PropertyMedia();
            media.setPropertyId(propertyId);
            media.setMediaType(item.mediaType());
            media.setMediaUrl(item.mediaUrl().trim());
            boolean primary = item.primary() && !primaryAssigned;
            if (primary) {
                primaryAssigned = true;
            }
            media.setPrimary(primary);
            media.setDisplayOrder(item.displayOrder() != null ? item.displayOrder() : order++);
            media.setCaption(item.caption());
            media.setCreatedAt(now);
            propertyMediaRepository.save(media);
        }

        if (!primaryAssigned) {
            List<PropertyMedia> saved = propertyMediaRepository.findByPropertyIdOrderByDisplayOrderAsc(propertyId);
            if (!saved.isEmpty()) {
                PropertyMedia first = saved.getFirst();
                first.setPrimary(true);
                propertyMediaRepository.save(first);
            }
        }
    }

    private boolean isPublishedStatus(PropertyStatus status) {
        return status != PropertyStatus.DRAFT && status != PropertyStatus.CLOSED;
    }

    private PropertySummaryResponse toSummary(Property property) {
        List<PropertyMedia> media = propertyMediaRepository.findByPropertyIdOrderByDisplayOrderAsc(property.getId());
        return propertyMapper.toSummary(property, propertyMapper.resolvePrimaryImageUrl(media));
    }

    private PropertyDetailResponse toDetail(Property property) {
        List<PropertyMedia> media = propertyMediaRepository.findByPropertyIdOrderByDisplayOrderAsc(property.getId());
        return propertyMapper.toDetail(property, media);
    }
}
