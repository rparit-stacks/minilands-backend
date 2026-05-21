package com.minilands.backend.repository;

import com.minilands.backend.entity.RoiDistribution;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RoiDistributionRepository extends MongoRepository<RoiDistribution, String> {

    List<RoiDistribution> findByPropertyIdOrderByCreatedAtDesc(String propertyId);

    Optional<RoiDistribution> findByPropertyIdAndDistributionYearAndDistributionMonth(
            String propertyId, Integer distributionYear, Integer distributionMonth);

    Optional<RoiDistribution> findFirstByPropertyIdOrderByDistributionYearDescDistributionMonthDesc(String propertyId);
}
