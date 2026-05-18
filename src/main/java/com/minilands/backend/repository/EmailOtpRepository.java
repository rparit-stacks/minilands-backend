package com.minilands.backend.repository;

import com.minilands.backend.entity.EmailOtp;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EmailOtpRepository extends MongoRepository<EmailOtp, String> {

    Optional<EmailOtp> findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(String email);
}
