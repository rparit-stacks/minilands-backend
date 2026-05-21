package com.minilands.backend.service.roi.impl;

import com.minilands.backend.dto.property.MonthlyPaymentExecuteRequest;
import com.minilands.backend.entity.Property;
import com.minilands.backend.entity.enums.DistributionFrequency;
import com.minilands.backend.entity.enums.PropertyStatus;
import com.minilands.backend.repository.PropertyRepository;
import com.minilands.backend.service.property.MonthlyPaymentDistributionService;
import com.minilands.backend.service.roi.RoiDistributionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class RoiDistributionServiceImpl implements RoiDistributionService {

    private static final Logger log = LoggerFactory.getLogger(RoiDistributionServiceImpl.class);

    private final PropertyRepository propertyRepository;
    private final MonthlyPaymentDistributionService monthlyPaymentDistributionService;

    public RoiDistributionServiceImpl(
            PropertyRepository propertyRepository,
            MonthlyPaymentDistributionService monthlyPaymentDistributionService) {
        this.propertyRepository = propertyRepository;
        this.monthlyPaymentDistributionService = monthlyPaymentDistributionService;
    }

    /**
     * Runs at 09:00 IST on the 1st of every month.
     * Accrues monthly payment since the last successful run (or first-run anchor) for ACTIVE properties
     * whose {@link DistributionFrequency} is due this calendar month.
     */
    @Scheduled(cron = "0 0 9 1 * *", zone = "Asia/Kolkata")
    @Override
    public void runMonthlyDistribution() {
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        log.info("Monthly payment distribution job started (calendar gate {})", now);

        List<Property> activeProperties = propertyRepository.findByStatus(PropertyStatus.ACTIVE);

        int success = 0, skipped = 0, failed = 0;
        for (Property property : activeProperties) {
            if (property.getMonthlyRent() == null || property.getMonthlyRent().signum() <= 0) {
                skipped++;
                continue;
            }
            if (!isDueThisMonth(property.getDistributionFrequency(), now)) {
                skipped++;
                continue;
            }
            try {
                monthlyPaymentDistributionService.distribute(
                        property.getId(), new MonthlyPaymentExecuteRequest(null, null));
                success++;
            } catch (IllegalArgumentException e) {
                log.debug("Skipping {} — {}", property.getName(), e.getMessage());
                skipped++;
            } catch (Exception e) {
                log.error("Monthly payment distribution failed for property {} ({}): {}",
                        property.getName(), property.getId(), e.getMessage(), e);
                failed++;
            }
        }

        log.info("Monthly payment distribution complete — success={}, skipped={}, failed={}", success, skipped, failed);
    }

    /**
     * Returns true if the given frequency means a payout is due in the given month.
     *
     * MONTHLY      → every month
     * QUARTERLY    → Jan, Apr, Jul, Oct
     * SEMI_ANNUAL  → Jan, Jul
     * ANNUAL       → Jan
     */
    private boolean isDueThisMonth(DistributionFrequency frequency, YearMonth period) {
        if (frequency == null || frequency == DistributionFrequency.MONTHLY) {
            return true;
        }
        int month = period.getMonthValue();
        return switch (frequency) {
            case QUARTERLY -> month == 1 || month == 4 || month == 7 || month == 10;
            case SEMI_ANNUAL -> month == 1 || month == 7;
            case ANNUAL -> month == 1;
            default -> true;
        };
    }
}
