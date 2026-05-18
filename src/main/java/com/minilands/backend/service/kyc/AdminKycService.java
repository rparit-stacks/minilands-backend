package com.minilands.backend.service.kyc;

import com.minilands.backend.dto.kyc.KycDocumentResponse;
import com.minilands.backend.dto.kyc.KycReviewRequest;

import java.util.List;

/**
 * Admin KYC review — not exposed to investor clients (ISP).
 */
public interface AdminKycService {

    List<KycDocumentResponse> listPendingDocuments();

    KycDocumentResponse reviewDocument(String adminId, String documentId, KycReviewRequest request);

    void approveUserKyc(String adminId, String userId);

    void rejectUserKyc(String adminId, String userId, String note);
}
