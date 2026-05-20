package com.minilands.backend.controller.investor;

import com.minilands.backend.dto.UserPrincipal;
import com.minilands.backend.dto.dashboard.DashboardResponse;
import com.minilands.backend.service.dashboard.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("[DashboardController] GET /api/dashboard — userId={}", principal.getUserId());
        DashboardResponse response = dashboardService.getSummary(principal.getUserId());
        log.info("[DashboardController] dashboard ok — totalInvested={} holdings={} txns={}",
                response.summary().totalInvested(), response.holdings().size(), response.recentTransactions().size());
        return ResponseEntity.ok(response);
    }
}
