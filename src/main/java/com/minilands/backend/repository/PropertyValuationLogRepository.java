package com.minilands.backend.repository;

import com.minilands.backend.entity.PropertyValuationLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PropertyValuationLogRepository extends MongoRepository<PropertyValuationLog, String> {

    List<PropertyValuationLog> findByPropertyIdOrderByValuedAtDesc(String propertyId);
}
