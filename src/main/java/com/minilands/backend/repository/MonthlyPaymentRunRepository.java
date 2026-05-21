package com.minilands.backend.repository;

import com.minilands.backend.entity.MonthlyPaymentRun;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface MonthlyPaymentRunRepository extends MongoRepository<MonthlyPaymentRun, String> {

    List<MonthlyPaymentRun> findByPropertyIdOrderByCreatedAtDesc(String propertyId);

    List<MonthlyPaymentRun> findByAccrualEndAfter(Instant instant);
}
