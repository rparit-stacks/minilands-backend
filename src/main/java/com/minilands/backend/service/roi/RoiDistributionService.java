package com.minilands.backend.service.roi;

/**
 * Scheduled monthly payment accrual (wallet credits) for eligible properties.
 */
public interface RoiDistributionService {

    void runMonthlyDistribution();
}
