package com.minilands.backend.repository;

import com.minilands.backend.entity.RoiEarning;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RoiEarningRepository extends MongoRepository<RoiEarning, String> {

    List<RoiEarning> findByUserIdOrderByEarnedOnDateDesc(String userId);

    List<RoiEarning> findByHoldingId(String holdingId);

    List<RoiEarning> findByPropertyId(String propertyId);
}
