package com.minilands.backend.controller.admin;

import com.minilands.backend.dto.wallet.AdminActionRequest;
import com.minilands.backend.dto.wallet.WithdrawalResponse;
import com.minilands.backend.security.AdminPrincipal;
import com.minilands.backend.service.wallet.AdminWithdrawalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/withdrawals")
public class AdminWithdrawalController {

    private final AdminWithdrawalService adminWithdrawalService;

    public AdminWithdrawalController(AdminWithdrawalService adminWithdrawalService) {
        this.adminWithdrawalService = adminWithdrawalService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<WithdrawalResponse>> listPending(
            @AuthenticationPrincipal AdminPrincipal principal) {
        return ResponseEntity.ok(adminWithdrawalService.listPendingWithdrawals());
    }

    @PostMapping("/{withdrawalId}/approve")
    public ResponseEntity<Void> approve(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String withdrawalId,
            @Valid @RequestBody(required = false) AdminActionRequest request) {
        adminWithdrawalService.approve(principal.getAdminId(), withdrawalId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{withdrawalId}/reject")
    public ResponseEntity<Void> reject(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String withdrawalId,
            @Valid @RequestBody AdminActionRequest request) {
        adminWithdrawalService.reject(principal.getAdminId(), withdrawalId, request);
        return ResponseEntity.ok().build();
    }
}
