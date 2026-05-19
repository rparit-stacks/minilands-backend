package com.minilands.backend.service.kyc.impl;

import com.minilands.backend.dto.kyc.KycDocumentResponse;
import com.minilands.backend.dto.kyc.KycReviewRequest;
import com.minilands.backend.entity.KycDocument;
import com.minilands.backend.entity.User;
import com.minilands.backend.entity.enums.ApprovalStatus;
import com.minilands.backend.entity.enums.KycDocumentType;
import com.minilands.backend.entity.enums.KycStatus;
import com.minilands.backend.entity.enums.NotificationType;
import com.minilands.backend.repository.KycDocumentRepository;
import com.minilands.backend.repository.UserRepository;
import com.minilands.backend.service.kyc.AdminKycService;
import com.minilands.backend.service.notification.NotificationService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminKycServiceImpl implements AdminKycService {

    private final KycDocumentRepository kycDocumentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AdminKycServiceImpl(
            KycDocumentRepository kycDocumentRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.kycDocumentRepository = kycDocumentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    public List<KycDocumentResponse> listPendingDocuments() {
        return kycDocumentRepository.findByStatus(ApprovalStatus.PENDING).stream()
                .map(KycSupport::toResponse)
                .toList();
    }

    @Override
    public KycDocumentResponse reviewDocument(String adminId, String documentId, KycReviewRequest request) {
        if (request.status() == ApprovalStatus.PENDING) {
            throw new IllegalArgumentException("Review status must be APPROVED or REJECTED");
        }

        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("KYC document not found"));

        if (document.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalArgumentException("Only pending documents can be reviewed");
        }

        Instant now = Instant.now();
        document.setStatus(request.status());
        document.setReviewedByAdminId(adminId);
        document.setReviewNote(request.note());
        document.setReviewedAt(now);
        document.setUpdatedAt(now);
        kycDocumentRepository.save(document);

        User user = findUser(document.getUserId());
        if (request.status() == ApprovalStatus.REJECTED) {
            user.setKycStatus(KycStatus.REJECTED);
            user.setKycRejectionNote(request.note());
            user.setKycVerifiedAt(null);
            user.setUpdatedAt(now);
            userRepository.save(user);

            notificationService.send(
                    user.getId(),
                    NotificationType.KYC,
                    "KYC document rejected",
                    "Your " + document.getDocumentType() + " was rejected."
                            + (request.note() != null ? " Reason: " + request.note() : "")
                            + " Please re-submit.");
        } else {
            notificationService.send(
                    user.getId(),
                    NotificationType.KYC,
                    "KYC document approved",
                    "Your " + document.getDocumentType() + " has been approved.");
        }

        return KycSupport.toResponse(document);
    }

    @Override
    public void approveUserKyc(String adminId, String userId) {
        User user = findUser(userId);
        if (user.getKycStatus() == KycStatus.APPROVED) {
            throw new IllegalArgumentException("KYC is already approved for this user");
        }

        Map<KycDocumentType, KycDocument> documentsByType = kycDocumentRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(KycDocument::getDocumentType, Function.identity()));

        for (KycDocumentType requiredType : KycSupport.REQUIRED_DOCUMENT_TYPES) {
            KycDocument document = documentsByType.get(requiredType);
            if (document == null) {
                throw new IllegalArgumentException("Missing required document: " + requiredType);
            }
            if (document.getStatus() != ApprovalStatus.APPROVED) {
                throw new IllegalArgumentException("Document " + requiredType + " is not approved yet");
            }
        }

        Instant now = Instant.now();
        user.setKycStatus(KycStatus.APPROVED);
        user.setKycVerifiedAt(now);
        user.setKycRejectionNote(null);
        user.setUpdatedAt(now);
        userRepository.save(user);

        notificationService.send(
                userId,
                NotificationType.KYC,
                "KYC approved",
                "Your KYC has been approved. You can now deposit and invest.");
    }

    @Override
    public void rejectUserKyc(String adminId, String userId, String note) {
        User user = findUser(userId);
        Instant now = Instant.now();

        List<KycDocument> documents = kycDocumentRepository.findByUserId(userId);
        for (KycDocument document : documents) {
            if (document.getStatus() == ApprovalStatus.PENDING) {
                document.setStatus(ApprovalStatus.REJECTED);
                document.setReviewedByAdminId(adminId);
                document.setReviewNote(note);
                document.setReviewedAt(now);
                document.setUpdatedAt(now);
            }
        }
        kycDocumentRepository.saveAll(documents);

        user.setKycStatus(KycStatus.REJECTED);
        user.setKycRejectionNote(note);
        user.setKycVerifiedAt(null);
        user.setUpdatedAt(now);
        userRepository.save(user);

        notificationService.send(
                userId,
                NotificationType.KYC,
                "KYC rejected",
                "Your KYC was rejected. " + note + " Please re-submit your documents.");
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
