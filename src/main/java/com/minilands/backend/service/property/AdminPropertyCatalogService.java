package com.minilands.backend.service.property;

import com.minilands.backend.dto.property.CreatePropertyRequest;
import com.minilands.backend.dto.property.PropertyDetailResponse;
import com.minilands.backend.dto.property.PropertySummaryResponse;
import com.minilands.backend.dto.property.UpdatePropertyRequest;
import com.minilands.backend.dto.property.UpdatePropertyStatusRequest;
import com.minilands.backend.entity.enums.PropertyStatus;

import java.util.List;

public interface AdminPropertyCatalogService {

    PropertyDetailResponse create(String adminId, CreatePropertyRequest request);

    PropertyDetailResponse update(String propertyId, UpdatePropertyRequest request);

    PropertyDetailResponse updateStatus(String propertyId, UpdatePropertyStatusRequest request);

    PropertyDetailResponse getById(String propertyId);

    List<PropertySummaryResponse> listAll(PropertyStatus status);
}
