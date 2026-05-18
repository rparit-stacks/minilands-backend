package com.minilands.backend.repository;

import com.minilands.backend.entity.Transaction;
import com.minilands.backend.entity.enums.TransactionType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TransactionRepository extends MongoRepository<Transaction, String> {

    List<Transaction> findByWalletId(String walletId);

    List<Transaction> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Transaction> findByUserIdAndType(String userId, TransactionType type);
}
