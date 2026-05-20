package com.minilands.backend.service.property;

import com.minilands.backend.dto.property.PropertyDetailResponse;
import com.minilands.backend.dto.property.UpdatePropertyValuationRequest;
import com.minilands.backend.dto.property.ValuationLogResponse;

import java.util.List;

public interface AdminPropertyValuationService {

    PropertyDetailResponse updateValuation(String adminId, String propertyId, UpdatePropertyValuationRequest request);

    List<ValuationLogResponse> getValuationHistory(String propertyId);
}
