package com.minilands.backend.service.roi;

/**
 * Monthly ROI calculation and wallet credit (SRP).
 * Scheduler and manual triggers depend on this abstraction (DIP).
 */
public interface RoiDistributionService {

    void runMonthlyDistribution();

    void distributeForProperty(String propertyId, int year, int month);
}
