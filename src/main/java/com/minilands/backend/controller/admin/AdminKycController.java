package com.minilands.backend.controller.admin;

import com.minilands.backend.dto.kyc.KycDocumentResponse;
import com.minilands.backend.dto.kyc.KycReviewRequest;
import com.minilands.backend.dto.kyc.RejectKycRequest;
import com.minilands.backend.security.AdminPrincipal;
import com.minilands.backend.service.kyc.AdminKycService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/kyc")
public class AdminKycController {

    private final AdminKycService adminKycService;

    public AdminKycController(AdminKycService adminKycService) {
        this.adminKycService = adminKycService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<KycDocumentResponse>> listPending(
            @AuthenticationPrincipal AdminPrincipal principal) {
        return ResponseEntity.ok(adminKycService.listPendingDocuments());
    }

    @PutMapping("/documents/{documentId}")
    public ResponseEntity<KycDocumentResponse> reviewDocument(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String documentId,
            @Valid @RequestBody KycReviewRequest request) {
        return ResponseEntity.ok(adminKycService.reviewDocument(principal.getAdminId(), documentId, request));
    }

    @PostMapping("/users/{userId}/approve")
    public ResponseEntity<Void> approveUser(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String userId) {
        adminKycService.approveUserKyc(principal.getAdminId(), userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{userId}/reject")
    public ResponseEntity<Void> rejectUser(
            @AuthenticationPrincipal AdminPrincipal principal,
            @PathVariable String userId,
            @Valid @RequestBody RejectKycRequest request) {
        adminKycService.rejectUserKyc(principal.getAdminId(), userId, request.note());
        return ResponseEntity.ok().build();
    }
}
