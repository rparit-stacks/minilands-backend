package com.minilands.backend.service.property;

import com.minilands.backend.dto.property.PropertyDetailResponse;
import com.minilands.backend.dto.property.PropertySummaryResponse;
import com.minilands.backend.entity.enums.PropertyStatus;
import com.minilands.backend.entity.enums.PropertyType;

import java.util.List;

/**
 * Read-only property browsing (ISP — separated from investment writes).
 */
public interface PropertyCatalogService {

    List<PropertySummaryResponse> listAvailable(
            PropertyStatus status,
            PropertyType propertyType,
            String city,
            Boolean featuredOnly);

    PropertyDetailResponse getById(String propertyId);

    PropertyDetailResponse getBySlug(String slug);
}
