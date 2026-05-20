package com.minilands.backend.controller.admin;

import com.minilands.backend.dto.payment.AdminDepositResponse;
import com.minilands.backend.entity.enums.DepositStatus;
import com.minilands.backend.security.AdminPrincipal;
import com.minilands.backend.service.payment.AdminDepositService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/deposits")
public class AdminDepositController {

    private final AdminDepositService adminDepositService;

    public AdminDepositController(AdminDepositService adminDepositService) {
        this.adminDepositService = adminDepositService;
    }

    /** GET /api/admin/deposits?status=CREATED  (omit param = all) */
    @GetMapping
    public ResponseEntity<List<AdminDepositResponse>> list(
            @AuthenticationPrincipal AdminPrincipal principal,
            @RequestParam(required = false) DepositStatus status) {
        return ResponseEntity.ok(adminDepositService.listDeposits(status));
    }

    @GetMapping("/{depositId}")
    public ResponseEntity<AdminDepositResponse> getById(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String depositId) {
        return ResponseEntity.ok(adminDepositService.getById(depositId));
    }

    /** Force a CREATED deposit to PAID and credit the user's wallet. */
    @PostMapping("/{depositId}/complete")
    public ResponseEntity<AdminDepositResponse> manuallyComplete(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String depositId) {
        return ResponseEntity.ok(adminDepositService.manuallyComplete(depositId));
    }
}
