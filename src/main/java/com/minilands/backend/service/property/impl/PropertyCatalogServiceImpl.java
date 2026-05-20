package com.minilands.backend.service.property.impl;

import com.minilands.backend.dto.property.PropertyDetailResponse;
import com.minilands.backend.dto.property.PropertyPageResponse;
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

import java.math.BigDecimal;
import java.util.Comparator;
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
    public PropertyPageResponse search(
            PropertyStatus status,
            PropertyType propertyType,
            String city,
            Boolean featuredOnly,
            String q,
            String sortBy,
            int page,
            int size) {

        List<Property> base = propertyRepository
                .findByStatusInAndListingVisibilityOrderByFeaturedDescDisplayOrderAscCreatedAtDesc(
                        INVESTOR_LIST_STATUSES, ListingVisibility.PUBLIC);

        String qLower = StringUtils.hasText(q) ? q.toLowerCase() : null;

        List<PropertySummaryResponse> filtered = base.stream()
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> propertyType == null || p.getPropertyType() == propertyType)
                .filter(p -> !StringUtils.hasText(city)
                        || (p.getLocation() != null && city.equalsIgnoreCase(p.getLocation().getCity())))
                .filter(p -> featuredOnly == null || !featuredOnly || p.isFeatured())
                .filter(p -> qLower == null
                        || (p.getName() != null && p.getName().toLowerCase().contains(qLower))
                        || (p.getTagline() != null && p.getTagline().toLowerCase().contains(qLower))
                        || (p.getShortDescription() != null && p.getShortDescription().toLowerCase().contains(qLower))
                        || (p.getLocation() != null && p.getLocation().getCity() != null
                                && p.getLocation().getCity().toLowerCase().contains(qLower)))
                .sorted(comparatorFor(sortBy))
                .map(this::toSummary)
                .toList();

        long total = filtered.size();
        int safeSize = size < 1 ? 10 : Math.min(size, 100);
        int safePage = Math.max(page, 0);
        int totalPages = (int) Math.ceil((double) total / safeSize);

        List<PropertySummaryResponse> pageContent = filtered.stream()
                .skip((long) safePage * safeSize)
                .limit(safeSize)
                .toList();

        return new PropertyPageResponse(pageContent, safePage, safeSize, total, totalPages);
    }

    private Comparator<Property> comparatorFor(String sortBy) {
        if (sortBy == null) return defaultOrder();
        return switch (sortBy.toLowerCase()) {
            case "roi" -> Comparator.comparing(
                    p -> p.getAnnualRoi() != null ? p.getAnnualRoi() : BigDecimal.ZERO,
                    Comparator.reverseOrder());
            case "price_asc" -> Comparator.comparing(
                    p -> p.getCurrentPrice() != null ? p.getCurrentPrice() : BigDecimal.ZERO);
            case "price_desc" -> Comparator.comparing(
                    (Property p) -> p.getCurrentPrice() != null ? p.getCurrentPrice() : BigDecimal.ZERO,
                    Comparator.reverseOrder());
            case "newest" -> Comparator.comparing(Property::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            default -> defaultOrder();
        };
    }

    private Comparator<Property> defaultOrder() {
        return Comparator.comparing(Property::isFeatured, Comparator.reverseOrder())
                .thenComparing(p -> p.getDisplayOrder() != null ? p.getDisplayOrder() : 0)
                .thenComparing(Property::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
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
