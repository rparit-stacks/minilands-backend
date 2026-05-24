package com.minilands.backend.repository;

import com.minilands.backend.entity.AdminInviteToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AdminInviteTokenRepository extends MongoRepository<AdminInviteToken, String> {

    Optional<AdminInviteToken> findByTokenHash(String tokenHash);
}
