package com.minilands.backend.repository;

import com.minilands.backend.entity.Withdrawal;
import com.minilands.backend.entity.enums.WithdrawalStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WithdrawalRepository extends MongoRepository<Withdrawal, String> {

    List<Withdrawal> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Withdrawal> findByStatus(WithdrawalStatus status);
}
