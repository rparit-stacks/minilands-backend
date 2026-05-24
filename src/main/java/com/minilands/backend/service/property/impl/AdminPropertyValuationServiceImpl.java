package com.minilands.backend.service.property.impl;

import com.minilands.backend.dto.property.PropertyDetailResponse;
import com.minilands.backend.dto.property.UpdatePropertyValuationRequest;
import com.minilands.backend.dto.property.ValuationLogResponse;
import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.PropertyValuationLog;
import com.minilands.backend.repository.PropertyMediaRepository;
import com.minilands.backend.repository.PropertyRepository;
import com.minilands.backend.repository.PropertyValuationLogRepository;
import com.minilands.backend.service.property.AdminPropertyValuationService;
import com.minilands.backend.service.property.PropertyInvestmentMath;
import com.minilands.backend.service.property.SharePriceValuationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class AdminPropertyValuationServiceImpl implements AdminPropertyValuationService {

    private final PropertyRepository propertyRepository;
    private final PropertyMediaRepository propertyMediaRepository;
    private final PropertyMapper propertyMapper;
    private final SharePriceValuationService sharePriceValuationService;
    private final PropertyValuationLogRepository valuationLogRepository;

    public AdminPropertyValuationServiceImpl(
            PropertyRepository propertyRepository,
            PropertyMediaRepository propertyMediaRepository,
            PropertyMapper propertyMapper,
            SharePriceValuationService sharePriceValuationService,
            PropertyValuationLogRepository valuationLogRepository) {
        this.propertyRepository = propertyRepository;
        this.propertyMediaRepository = propertyMediaRepository;
        this.propertyMapper = propertyMapper;
        this.sharePriceValuationService = sharePriceValuationService;
        this.valuationLogRepository = valuationLogRepository;
    }

    @Override
    @Transactional
    public PropertyDetailResponse updateValuation(String adminId, String propertyId, UpdatePropertyValuationRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        Instant now = Instant.now();

        BigDecimal oldTotal = property.getTotalValue();
        BigDecimal newTotal = request.totalValue();

        PropertyValuationLog log = new PropertyValuationLog();
        log.setPropertyId(propertyId);
        log.setPreviousTotalValue(oldTotal);
        log.setPreviousSharePrice(property.getCurrentPrice());
        log.setNewTotalValue(newTotal);
        log.setUpdatedByAdminId(adminId);
        log.setNote(request.note());
        log.setValuedAt(now);

        property.setTotalValue(newTotal);
        property.setTotalTarget(newTotal);
        property.setLastValuationDate(now);
        property.setSharePrice(PropertyInvestmentMath.deriveSharePrice(newTotal, property.getTotalShares()));
        property.setCurrentPrice(sharePriceValuationService.getEstimatedSharePrice(property));
        property.setUpdatedAt(now);

        log.setNewSharePrice(property.getCurrentPrice());
        valuationLogRepository.save(log);

        Property saved = propertyRepository.save(property);
        var media = propertyMediaRepository.findByPropertyIdOrderByDisplayOrderAsc(saved.getId());
        return propertyMapper.toDetail(saved, media);
    }

    @Override
    public List<ValuationLogResponse> getValuationHistory(String propertyId) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new IllegalArgumentException("Property not found");
        }
        return valuationLogRepository.findByPropertyIdOrderByValuedAtDesc(propertyId).stream()
                .map(l -> new ValuationLogResponse(
                        l.getId(),
                        l.getPreviousTotalValue(),
                        l.getNewTotalValue(),
                        l.getPreviousSharePrice(),
                        l.getNewSharePrice(),
                        l.getUpdatedByAdminId(),
                        l.getNote(),
                        l.getValuedAt()))
                .toList();
    }
}
