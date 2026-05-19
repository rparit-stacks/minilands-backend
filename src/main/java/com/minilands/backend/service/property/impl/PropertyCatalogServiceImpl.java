package com.minilands.backend.service.property.impl;

import com.minilands.backend.dto.property.PropertyDetailResponse;
import com.minilands.backend.dto.property.PropertySummaryResponse;
import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.PropertyMedia;
import com.minilands.backend.entity.enums.ListingVisibility;
import com.minilands.backend.entity.enums.PropertyStatus;
import com.minilands.backend.entity.enums.PropertyType;
import com.minilands.backend.repository.PropertyMediaRepository;
import com.minilands.backend.repository.PropertyRepository;
import com.minilands.backend.service.property.PropertyCatalogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class PropertyCatalogServiceImpl implements PropertyCatalogService {

    private static final Set<PropertyStatus> INVESTOR_LIST_STATUSES = EnumSet.of(
            PropertyStatus.COMING_SOON,
            PropertyStatus.OPEN,
            PropertyStatus.FUNDED,
            PropertyStatus.ACTIVE);

    private final PropertyRepository propertyRepository;
    private final PropertyMediaRepository propertyMediaRepository;
    private final PropertyMapper propertyMapper;

    public PropertyCatalogServiceImpl(
            PropertyRepository propertyRepository,
            PropertyMediaRepository propertyMediaRepository,
            PropertyMapper propertyMapper) {
        this.propertyRepository = propertyRepository;
        this.propertyMediaRepository = propertyMediaRepository;
        this.propertyMapper = propertyMapper;
    }

    @Override
    public List<PropertySummaryResponse> listAvailable(
            PropertyStatus status,
            PropertyType propertyType,
            String city,
            Boolean featuredOnly) {
        List<Property> properties = propertyRepository
                .findByStatusInAndListingVisibilityOrderByFeaturedDescDisplayOrderAscCreatedAtDesc(
                        INVESTOR_LIST_STATUSES,
                        ListingVisibility.PUBLIC);

        return properties.stream()
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> propertyType == null || p.getPropertyType() == propertyType)
                .filter(p -> !StringUtils.hasText(city)
                        || (p.getLocation() != null
                        && city.equalsIgnoreCase(p.getLocation().getCity())))
                .filter(p -> featuredOnly == null || !featuredOnly || p.isFeatured())
                .map(this::toSummary)
                .toList();
    }

    @Override
    public PropertyDetailResponse getById(String propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
        assertInvestorVisible(property);
        return toDetail(property);
    }

    @Override
    public PropertyDetailResponse getBySlug(String slug) {
        Property property = propertyRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
        assertInvestorVisible(property);
        return toDetail(property);
    }

    private void assertInvestorVisible(Property property) {
        if (property.getListingVisibility() != ListingVisibility.PUBLIC) {
            throw new IllegalArgumentException("Property is not available");
        }
        if (!INVESTOR_LIST_STATUSES.contains(property.getStatus())) {
            throw new IllegalArgumentException("Property is not available for viewing");
        }
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
