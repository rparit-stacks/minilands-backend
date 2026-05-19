package com.minilands.backend.service.property.impl;

import com.minilands.backend.dto.property.PropertyDetailResponse;
import com.minilands.backend.dto.property.UpdatePropertyValuationRequest;
import com.minilands.backend.entity.Property;
import com.minilands.backend.repository.PropertyMediaRepository;
import com.minilands.backend.repository.PropertyRepository;
import com.minilands.backend.service.property.AdminPropertyValuationService;
import com.minilands.backend.service.property.PropertyInvestmentMath;
import com.minilands.backend.service.property.SharePriceValuationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AdminPropertyValuationServiceImpl implements AdminPropertyValuationService {

    private final PropertyRepository propertyRepository;
    private final PropertyMediaRepository propertyMediaRepository;
    private final PropertyMapper propertyMapper;
    private final SharePriceValuationService sharePriceValuationService;

    public AdminPropertyValuationServiceImpl(
            PropertyRepository propertyRepository,
            PropertyMediaRepository propertyMediaRepository,
            PropertyMapper propertyMapper,
            SharePriceValuationService sharePriceValuationService) {
        this.propertyRepository = propertyRepository;
        this.propertyMediaRepository = propertyMediaRepository;
        this.propertyMapper = propertyMapper;
        this.sharePriceValuationService = sharePriceValuationService;
    }

    @Override
    @Transactional
    public PropertyDetailResponse updateValuation(String propertyId, UpdatePropertyValuationRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        Instant now = Instant.now();
        property.setTotalValue(request.totalValue());
        property.setTotalTarget(request.totalValue());
        property.setLastValuationDate(now);
        property.setSharePrice(PropertyInvestmentMath.deriveSharePrice(
                request.totalValue(), property.getTotalShares()));
        property.setCurrentPrice(sharePriceValuationService.getEstimatedSharePrice(property));
        property.setUpdatedAt(now);

        Property saved = propertyRepository.save(property);
        var media = propertyMediaRepository.findByPropertyIdOrderByDisplayOrderAsc(saved.getId());
        return propertyMapper.toDetail(saved, media);
    }
}
