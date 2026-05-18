package com.minilands.backend.service.dashboard;

import com.minilands.backend.dto.dashboard.DashboardResponse;

/**
 * Portfolio summary and analytics aggregation (SRP).
 */
public interface DashboardService {

    DashboardResponse getSummary(String userId);
}
