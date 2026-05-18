package com.minilands.backend.repository;

import com.minilands.backend.entity.AdminRefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRefreshTokenRepository extends MongoRepository<AdminRefreshToken, String> {

    Optional<AdminRefreshToken> findByTokenHash(String tokenHash);

    List<AdminRefreshToken> findByAdminId(String adminId);

    void deleteByAdminId(String adminId);
}
