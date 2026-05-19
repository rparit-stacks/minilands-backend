package com.minilands.backend.service.property;

import com.minilands.backend.dto.property.DistributeRentRequest;
import com.minilands.backend.dto.property.RentDistributionResponse;

public interface PropertyRentDistributionService {

    RentDistributionResponse distributeRent(String propertyId, DistributeRentRequest request);
}
