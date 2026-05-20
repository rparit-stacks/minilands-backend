package com.minilands.backend.controller.admin;

import com.minilands.backend.dto.voting.AdminProposalActionRequest;
import com.minilands.backend.dto.voting.ProposalResponse;
import com.minilands.backend.security.AdminPrincipal;
import com.minilands.backend.service.voting.AdminPropertySaleService;
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
@RequestMapping("/api/admin/proposals")
public class AdminPropertySaleController {

    private final AdminPropertySaleService adminPropertySaleService;

    public AdminPropertySaleController(AdminPropertySaleService adminPropertySaleService) {
        this.adminPropertySaleService = adminPropertySaleService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ProposalResponse>> listPending(
            @AuthenticationPrincipal AdminPrincipal principal) {
        return ResponseEntity.ok(adminPropertySaleService.listPendingApproval());
    }

    @PostMapping("/{proposalId}/approve")
    public ResponseEntity<ProposalResponse> approve(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String proposalId,
            @RequestBody(required = false) AdminProposalActionRequest request) {
        String note = request != null ? request.note() : null;
        return ResponseEntity.ok(adminPropertySaleService.approveProposal(principal.getAdminId(), proposalId, note));
    }

    @PostMapping("/{proposalId}/reject")
    public ResponseEntity<ProposalResponse> reject(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String proposalId,
            @RequestBody(required = false) AdminProposalActionRequest request) {
        String note = request != null ? request.note() : null;
        return ResponseEntity.ok(adminPropertySaleService.rejectProposal(principal.getAdminId(), proposalId, note));
    }
}
