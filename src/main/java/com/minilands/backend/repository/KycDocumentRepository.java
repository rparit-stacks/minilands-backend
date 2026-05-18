package com.minilands.backend.repository;

import com.minilands.backend.entity.KycDocument;
import com.minilands.backend.entity.enums.ApprovalStatus;
import com.minilands.backend.entity.enums.KycDocumentType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface KycDocumentRepository extends MongoRepository<KycDocument, String> {

    List<KycDocument> findByUserId(String userId);

    Optional<KycDocument> findByUserIdAndDocumentType(String userId, KycDocumentType documentType);

    List<KycDocument> findByStatus(ApprovalStatus status);
}
