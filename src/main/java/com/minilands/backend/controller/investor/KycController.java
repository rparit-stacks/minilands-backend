package com.minilands.backend.controller.investor;

import com.minilands.backend.dto.kyc.KycDocumentResponse;
import com.minilands.backend.dto.kyc.KycStatusResponse;
import com.minilands.backend.dto.kyc.SubmitKycDocumentRequest;
import com.minilands.backend.dto.UserPrincipal;
import com.minilands.backend.service.kyc.KycService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/kyc")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping("/documents")
    public ResponseEntity<KycDocumentResponse> submitDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubmitKycDocumentRequest request) {
        return ResponseEntity.ok(kycService.submitDocument(principal.getUserId(), request));
    }

    @GetMapping("/documents")
    public ResponseEntity<List<KycDocumentResponse>> getDocuments(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(kycService.getDocuments(principal.getUserId()));
    }

    @GetMapping("/status")
    public ResponseEntity<KycStatusResponse> getStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(kycService.getStatus(principal.getUserId()));
    }
}
