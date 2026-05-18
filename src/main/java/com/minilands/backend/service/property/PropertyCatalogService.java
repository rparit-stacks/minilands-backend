package com.minilands.backend.service.property;

import com.minilands.backend.dto.property.PropertyDetailResponse;
import com.minilands.backend.dto.property.PropertySummaryResponse;

import java.util.List;

/**
 * Read-only property browsing (ISP — separated from investment writes).
 */
public interface PropertyCatalogService {

    List<PropertySummaryResponse> listAvailable();

    PropertyDetailResponse getById(String propertyId);
}
