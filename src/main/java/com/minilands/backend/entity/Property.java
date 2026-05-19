package com.minilands.backend.entity;

import com.minilands.backend.entity.embeddable.PropertyDocumentRef;
import com.minilands.backend.entity.embeddable.PropertyLocation;
import com.minilands.backend.entity.enums.AssetClass;
import com.minilands.backend.entity.enums.DistributionFrequency;
import com.minilands.backend.entity.enums.LegalStatus;
import com.minilands.backend.entity.enums.ListingVisibility;
import com.minilands.backend.entity.enums.PropertyStatus;
import com.minilands.backend.entity.enums.PropertyType;
import com.minilands.backend.entity.enums.ValuationFrequency;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "properties")
public class Property {

    @Id
    private String id;

    @Indexed(unique = true)
    private String slug;

    private String name;
    private String tagline;
    private String shortDescription;
    private String description;

    private PropertyType propertyType;
    private AssetClass assetClass;

    private PropertyLocation location;

    private String currency;
    /** Total property value (same as totalTarget for fundraising). */
    private BigDecimal totalValue;
    private BigDecimal totalTarget;
    private Integer totalShares;
    private BigDecimal sharePrice;
    private BigDecimal currentPrice;
    private BigDecimal minInvestmentAmount;
    private Integer maxSharesPerInvestor;
    private BigDecimal sharesSold;

    private BigDecimal annualRoi;
    private BigDecimal monthlyRoi;
    private BigDecimal projectedAnnualYield;
    private BigDecimal rentalYieldPercent;
    private BigDecimal expectedIrrPercent;
    private BigDecimal appreciationRatePercent;
    private DistributionFrequency distributionFrequency;
    private Integer holdPeriodMonths;

    private String developerName;
    private String builderName;
    private String spvName;
    private String reraRegistrationId;
    private Integer yearBuilt;
    private BigDecimal carpetAreaSqFt;
    private BigDecimal builtUpAreaSqFt;
    private BigDecimal plotAreaSqFt;
    private Integer floors;
    private Integer totalUnits;
    private List<String> amenities = new ArrayList<>();
    private List<String> highlights = new ArrayList<>();

    private LegalStatus legalStatus;
    private boolean titleVerified;
    private String dueDiligenceSummary;
    private List<PropertyDocumentRef> documents = new ArrayList<>();

    private PropertyStatus status;
    private ListingVisibility listingVisibility;
    private boolean featured;
    private Integer displayOrder;
    private Instant fundingDeadline;
    private Instant fundedAt;
    private Instant publishedAt;

    private ValuationFrequency valuationFrequency;
    private Instant lastValuationDate;
    private BigDecimal monthlyRent;

    private Integer currentInvestors;
    private BigDecimal totalRaised;

    private String createdByAdminId;
    private Instant createdAt;
    private Instant updatedAt;

    public Property() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(PropertyType propertyType) {
        this.propertyType = propertyType;
    }

    public AssetClass getAssetClass() {
        return assetClass;
    }

    public void setAssetClass(AssetClass assetClass) {
        this.assetClass = assetClass;
    }

    public PropertyLocation getLocation() {
        return location;
    }

    public void setLocation(PropertyLocation location) {
        this.location = location;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotalValue() {
        return totalValue != null ? totalValue : totalTarget;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getTotalTarget() {
        return totalTarget;
    }

    public void setTotalTarget(BigDecimal totalTarget) {
        this.totalTarget = totalTarget;
    }

    public Integer getTotalShares() {
        return totalShares;
    }

    public void setTotalShares(Integer totalShares) {
        this.totalShares = totalShares;
    }

    public BigDecimal getSharePrice() {
        return sharePrice;
    }

    public void setSharePrice(BigDecimal sharePrice) {
        this.sharePrice = sharePrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getMinInvestmentAmount() {
        return minInvestmentAmount;
    }

    public void setMinInvestmentAmount(BigDecimal minInvestmentAmount) {
        this.minInvestmentAmount = minInvestmentAmount;
    }

    public Integer getMaxSharesPerInvestor() {
        return maxSharesPerInvestor;
    }

    public void setMaxSharesPerInvestor(Integer maxSharesPerInvestor) {
        this.maxSharesPerInvestor = maxSharesPerInvestor;
    }

    public BigDecimal getSharesSold() {
        return sharesSold;
    }

    public void setSharesSold(BigDecimal sharesSold) {
        this.sharesSold = sharesSold;
    }

    public BigDecimal getAnnualRoi() {
        return annualRoi;
    }

    public void setAnnualRoi(BigDecimal annualRoi) {
        this.annualRoi = annualRoi;
    }

    public BigDecimal getMonthlyRoi() {
        return monthlyRoi;
    }

    public void setMonthlyRoi(BigDecimal monthlyRoi) {
        this.monthlyRoi = monthlyRoi;
    }

    public BigDecimal getProjectedAnnualYield() {
        return projectedAnnualYield;
    }

    public void setProjectedAnnualYield(BigDecimal projectedAnnualYield) {
        this.projectedAnnualYield = projectedAnnualYield;
    }

    public BigDecimal getRentalYieldPercent() {
        return rentalYieldPercent;
    }

    public void setRentalYieldPercent(BigDecimal rentalYieldPercent) {
        this.rentalYieldPercent = rentalYieldPercent;
    }

    public BigDecimal getExpectedIrrPercent() {
        return expectedIrrPercent;
    }

    public void setExpectedIrrPercent(BigDecimal expectedIrrPercent) {
        this.expectedIrrPercent = expectedIrrPercent;
    }

    public BigDecimal getAppreciationRatePercent() {
        return appreciationRatePercent;
    }

    public void setAppreciationRatePercent(BigDecimal appreciationRatePercent) {
        this.appreciationRatePercent = appreciationRatePercent;
    }

    public DistributionFrequency getDistributionFrequency() {
        return distributionFrequency;
    }

    public void setDistributionFrequency(DistributionFrequency distributionFrequency) {
        this.distributionFrequency = distributionFrequency;
    }

    public Integer getHoldPeriodMonths() {
        return holdPeriodMonths;
    }

    public void setHoldPeriodMonths(Integer holdPeriodMonths) {
        this.holdPeriodMonths = holdPeriodMonths;
    }

    public String getDeveloperName() {
        return developerName;
    }

    public void setDeveloperName(String developerName) {
        this.developerName = developerName;
    }

    public String getBuilderName() {
        return builderName;
    }

    public void setBuilderName(String builderName) {
        this.builderName = builderName;
    }

    public String getSpvName() {
        return spvName;
    }

    public void setSpvName(String spvName) {
        this.spvName = spvName;
    }

    public String getReraRegistrationId() {
        return reraRegistrationId;
    }

    public void setReraRegistrationId(String reraRegistrationId) {
        this.reraRegistrationId = reraRegistrationId;
    }

    public Integer getYearBuilt() {
        return yearBuilt;
    }

    public void setYearBuilt(Integer yearBuilt) {
        this.yearBuilt = yearBuilt;
    }

    public BigDecimal getCarpetAreaSqFt() {
        return carpetAreaSqFt;
    }

    public void setCarpetAreaSqFt(BigDecimal carpetAreaSqFt) {
        this.carpetAreaSqFt = carpetAreaSqFt;
    }

    public BigDecimal getBuiltUpAreaSqFt() {
        return builtUpAreaSqFt;
    }

    public void setBuiltUpAreaSqFt(BigDecimal builtUpAreaSqFt) {
        this.builtUpAreaSqFt = builtUpAreaSqFt;
    }

    public BigDecimal getPlotAreaSqFt() {
        return plotAreaSqFt;
    }

    public void setPlotAreaSqFt(BigDecimal plotAreaSqFt) {
        this.plotAreaSqFt = plotAreaSqFt;
    }

    public Integer getFloors() {
        return floors;
    }

    public void setFloors(Integer floors) {
        this.floors = floors;
    }

    public Integer getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(Integer totalUnits) {
        this.totalUnits = totalUnits;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities != null ? amenities : new ArrayList<>();
    }

    public List<String> getHighlights() {
        return highlights;
    }

    public void setHighlights(List<String> highlights) {
        this.highlights = highlights != null ? highlights : new ArrayList<>();
    }

    public LegalStatus getLegalStatus() {
        return legalStatus;
    }

    public void setLegalStatus(LegalStatus legalStatus) {
        this.legalStatus = legalStatus;
    }

    public boolean isTitleVerified() {
        return titleVerified;
    }

    public void setTitleVerified(boolean titleVerified) {
        this.titleVerified = titleVerified;
    }

    public String getDueDiligenceSummary() {
        return dueDiligenceSummary;
    }

    public void setDueDiligenceSummary(String dueDiligenceSummary) {
        this.dueDiligenceSummary = dueDiligenceSummary;
    }

    public List<PropertyDocumentRef> getDocuments() {
        return documents;
    }

    public void setDocuments(List<PropertyDocumentRef> documents) {
        this.documents = documents != null ? documents : new ArrayList<>();
    }

    public PropertyStatus getStatus() {
        return status;
    }

    public void setStatus(PropertyStatus status) {
        this.status = status;
    }

    public ListingVisibility getListingVisibility() {
        return listingVisibility;
    }

    public void setListingVisibility(ListingVisibility listingVisibility) {
        this.listingVisibility = listingVisibility;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Instant getFundingDeadline() {
        return fundingDeadline;
    }

    public void setFundingDeadline(Instant fundingDeadline) {
        this.fundingDeadline = fundingDeadline;
    }

    public Instant getFundedAt() {
        return fundedAt;
    }

    public void setFundedAt(Instant fundedAt) {
        this.fundedAt = fundedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public ValuationFrequency getValuationFrequency() {
        return valuationFrequency;
    }

    public void setValuationFrequency(ValuationFrequency valuationFrequency) {
        this.valuationFrequency = valuationFrequency;
    }

    public Instant getLastValuationDate() {
        return lastValuationDate;
    }

    public void setLastValuationDate(Instant lastValuationDate) {
        this.lastValuationDate = lastValuationDate;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(BigDecimal monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public Integer getCurrentInvestors() {
        return currentInvestors;
    }

    public void setCurrentInvestors(Integer currentInvestors) {
        this.currentInvestors = currentInvestors;
    }

    public BigDecimal getTotalRaised() {
        return totalRaised;
    }

    public void setTotalRaised(BigDecimal totalRaised) {
        this.totalRaised = totalRaised;
    }

    public String getCreatedByAdminId() {
        return createdByAdminId;
    }

    public void setCreatedByAdminId(String createdByAdminId) {
        this.createdByAdminId = createdByAdminId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** Legacy single-line location for backward-compatible queries. */
    public String getLocationDisplay() {
        return location != null ? location.displayLocation() : null;
    }
}
