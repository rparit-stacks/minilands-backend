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
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
        Instant valuationAnchor = property.getLastValuationDate() != null
                ? property.getLastValuationDate()
                : property.getCreatedAt();
        if (valuationAnchor == null) {
            valuationAnchor = now;
        }
        long days = ChronoUnit.DAYS.between(
                valuationAnchor.atZone(ZoneOffset.UTC).toLocalDate(),
                now.atZone(ZoneOffset.UTC).toLocalDate());
        if (days < 1) {
            days = 1;
        }
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
        deriveAndSetCapitalGrowthRoi(property, oldTotal, newTotal, days);
        property.setUpdatedAt(now);

        log.setNewSharePrice(property.getCurrentPrice());
        valuationLogRepository.save(log);

        Property saved = propertyRepository.save(property);
        var media = propertyMediaRepository.findByPropertyIdOrderByDisplayOrderAsc(saved.getId());
        return propertyMapper.toDetail(saved, media);
    }

    /**
     * Sets {@code annualRoi} / {@code monthlyRoi} from implied capital appreciation only (valuation path).
     * Rental yield stays on its own axis (distributions); not mixed here.
     */
    private static void deriveAndSetCapitalGrowthRoi(Property property, BigDecimal oldTotal, BigDecimal newTotal, long days) {
        if (oldTotal == null || oldTotal.compareTo(BigDecimal.ZERO) <= 0 || newTotal == null) {
            return;
        }
        if (newTotal.compareTo(oldTotal) == 0) {
            property.setAnnualRoi(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            property.setMonthlyRoi(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            return;
        }
        BigDecimal annual = annualizedPercent(oldTotal, newTotal, days);
        if (annual != null) {
            property.setAnnualRoi(annual);
            property.setMonthlyRoi(annual.divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP));
        }
    }

    /** Annualized % return implied by value change over {@code days} (treated as fraction of a year). */
    private static BigDecimal annualizedPercent(BigDecimal oldV, BigDecimal newV, long days) {
        long d = Math.max(1, days);
        BigDecimal ratio = newV.divide(oldV, 14, RoundingMode.HALF_UP);
        double r = ratio.doubleValue();
        if (r <= 0 || !Double.isFinite(r)) {
            return null;
        }
        double years = d / 365.0;
        if (years <= 1e-9) {
            years = 1e-9;
        }
        double apy = (Math.pow(r, 1.0 / years) - 1.0) * 100.0;
        if (!Double.isFinite(apy)) {
            return null;
        }
        double capped = Math.max(-1_000_000, Math.min(1_000_000, apy));
        return BigDecimal.valueOf(capped).setScale(2, RoundingMode.HALF_UP);
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
