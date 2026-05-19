package com.minilands.backend.service.kyc.impl;

import com.minilands.backend.dto.kyc.KycDocumentResponse;
import com.minilands.backend.dto.kyc.KycStatusResponse;
import com.minilands.backend.dto.kyc.SubmitKycDocumentRequest;
import com.minilands.backend.entity.KycDocument;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.enums.ApprovalStatus;
import com.minilands.backend.entity.enums.KycDocumentType;
import com.minilands.backend.entity.enums.KycStatus;
import com.minilands.backend.entity.enums.NotificationType;
import com.minilands.backend.repository.KycDocumentRepository;
import com.minilands.backend.repository.UserRepository;
import com.minilands.backend.service.kyc.KycService;
import com.minilands.backend.service.notification.NotificationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KycServiceImpl implements KycService {

    private final UserRepository userRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final NotificationService notificationService;

    public KycServiceImpl(
            UserRepository userRepository,
            KycDocumentRepository kycDocumentRepository,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.kycDocumentRepository = kycDocumentRepository;
        this.notificationService = notificationService;
    }

    @Override
    public KycDocumentResponse submitDocument(String userId, SubmitKycDocumentRequest request) {
        User user = findUser(userId);
        if (user.getKycStatus() == KycStatus.APPROVED) {
            throw new IllegalArgumentException("KYC is already approved");
        }

        String documentUrl = request.documentUrl().trim();
        validateDocumentUrl(documentUrl);

        KycDocument document = kycDocumentRepository
                .findByUserIdAndDocumentType(userId, request.documentType())
                .orElseGet(KycDocument::new);

        if (document.getStatus() == ApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("This document is already approved and cannot be changed");
        }

        Instant now = Instant.now();
        if (document.getId() == null) {
            document.setUserId(userId);
            document.setDocumentType(request.documentType());
            document.setCreatedAt(now);
        }
        document.setDocumentUrl(documentUrl);
        document.setStatus(ApprovalStatus.PENDING);
        document.setReviewedByAdminId(null);
        document.setReviewNote(null);
        document.setReviewedAt(null);
        document.setUpdatedAt(now);
        kycDocumentRepository.save(document);

        user.setKycStatus(KycStatus.PENDING);
        user.setKycRejectionNote(null);
        user.setKycVerifiedAt(null);
        user.setUpdatedAt(now);
        userRepository.save(user);

        notificationService.send(
                userId,
                NotificationType.KYC,
                "KYC document submitted",
                "Your " + formatDocumentType(request.documentType()) + " has been submitted and is under review.");

        return KycSupport.toResponse(document);
    }

    @Override
    public List<KycDocumentResponse> getDocuments(String userId) {
        findUser(userId);
        return kycDocumentRepository.findByUserId(userId).stream()
                .map(KycSupport::toResponse)
                .toList();
    }

    @Override
    public KycStatusResponse getStatus(String userId) {
        User user = findUser(userId);
        List<KycDocumentResponse> documents = getDocuments(userId);
        Set<KycDocumentType> submittedTypes = documents.stream()
                .map(KycDocumentResponse::documentType)
                .collect(Collectors.toSet());

        return new KycStatusResponse(
                user.getKycStatus(),
                user.getKycVerifiedAt(),
                user.getKycRejectionNote(),
                submittedTypes.containsAll(KycSupport.REQUIRED_DOCUMENT_TYPES),
                documents);
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private void validateDocumentUrl(String documentUrl) {
        if (!documentUrl.startsWith("http://") && !documentUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Document URL must be a valid http(s) URL");
        }
    }

    private String formatDocumentType(KycDocumentType type) {
        return switch (type) {
            case AADHAR -> "Aadhar";
            case PAN -> "PAN";
            case SELFIE -> "Selfie";
        };
    }
}
