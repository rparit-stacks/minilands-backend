package com.minilands.backend.controller.investor;

import com.minilands.backend.dto.kyc.KycDocumentResponse;
import com.minilands.backend.dto.kyc.KycStatusResponse;
import com.minilands.backend.dto.kyc.SubmitKycDocumentRequest;
import com.minilands.backend.dto.UserPrincipal;
import com.minilands.backend.service.kyc.KycService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(KycController.class);

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping("/documents")
    public ResponseEntity<KycDocumentResponse> submitDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubmitKycDocumentRequest request) {
        log.info("[KycController] POST /api/kyc/documents — userId={} documentType={} url={}",
                principal.getUserId(), request.documentType(), request.documentUrl());
        KycDocumentResponse response = kycService.submitDocument(principal.getUserId(), request);
        log.info("[KycController] submitDocument ok — docId={} status={}", response.id(), response.status());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/documents")
    public ResponseEntity<List<KycDocumentResponse>> getDocuments(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("[KycController] GET /api/kyc/documents — userId={}", principal.getUserId());
        List<KycDocumentResponse> docs = kycService.getDocuments(principal.getUserId());
        log.info("[KycController] getDocuments ok — count={}", docs.size());
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/status")
    public ResponseEntity<KycStatusResponse> getStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("[KycController] GET /api/kyc/status — userId={}", principal.getUserId());
        KycStatusResponse response = kycService.getStatus(principal.getUserId());
        log.info("[KycController] getStatus ok — kycStatus={} docsCount={}", response.kycStatus(), response.documents().size());
        return ResponseEntity.ok(response);
    }
}
