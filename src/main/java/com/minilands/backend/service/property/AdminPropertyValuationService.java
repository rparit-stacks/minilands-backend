package com.minilands.backend.service.property;

import com.minilands.backend.dto.property.PropertyDetailResponse;
import com.minilands.backend.dto.property.UpdatePropertyValuationRequest;

public interface AdminPropertyValuationService {

    PropertyDetailResponse updateValuation(String propertyId, UpdatePropertyValuationRequest request);
}
