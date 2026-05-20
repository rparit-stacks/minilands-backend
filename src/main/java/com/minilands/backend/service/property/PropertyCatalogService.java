package com.minilands.backend.service.property;

import com.minilands.backend.dto.property.PropertyDetailResponse;
import com.minilands.backend.dto.property.PropertyPageResponse;
import com.minilands.backend.dto.property.PropertySummaryResponse;
import com.minilands.backend.entity.enums.PropertyStatus;
import com.minilands.backend.entity.enums.PropertyType;

import java.util.List;

public interface PropertyCatalogService {

    /**
     * Paginated, searchable, sortable property listing.
     * sortBy: "roi" | "price_asc" | "price_desc" | "newest" (default)
     */
    PropertyPageResponse search(
            PropertyStatus status,
            PropertyType propertyType,
            String city,
            Boolean featuredOnly,
            String q,
            String sortBy,
            int page,
            int size);

    List<PropertySummaryResponse> listAvailable(
            PropertyStatus status,
            PropertyType propertyType,
            String city,
            Boolean featuredOnly);

    PropertyDetailResponse getById(String propertyId);

    PropertyDetailResponse getBySlug(String slug);
}
