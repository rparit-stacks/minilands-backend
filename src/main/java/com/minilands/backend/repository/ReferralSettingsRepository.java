package com.minilands.backend.repository;

import com.minilands.backend.entity.ReferralSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReferralSettingsRepository extends MongoRepository<ReferralSettings, String> {
}
