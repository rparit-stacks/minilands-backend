package com.minilands.backend.repository;

import com.minilands.backend.entity.User;
import com.minilands.backend.entity.enums.KycStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByReferralCode(String referralCode);

    boolean existsByReferralCode(String referralCode);

    List<User> findByReferredByUserId(String referredByUserId);

    long countByReferredByUserId(String referredByUserId);

    List<User> findByKycStatus(KycStatus kycStatus);

    List<User> findByAccountStatus(com.minilands.backend.entity.enums.AccountStatus accountStatus);

    long countByAccountStatus(com.minilands.backend.entity.enums.AccountStatus accountStatus);
}
