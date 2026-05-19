package com.minilands.backend.service.kyc;

import com.minilands.backend.dto.kyc.KycDocumentResponse;
import com.minilands.backend.dto.kyc.KycStatusResponse;
import com.minilands.backend.dto.kyc.SubmitKycDocumentRequest;

import java.util.List;

/**
 * Investor-side KYC document submission (SRP).
 */
public interface KycService {

    KycDocumentResponse submitDocument(String userId, SubmitKycDocumentRequest request);

    List<KycDocumentResponse> getDocuments(String userId);

    KycStatusResponse getStatus(String userId);
}
