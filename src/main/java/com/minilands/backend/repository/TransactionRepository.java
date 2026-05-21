package com.minilands.backend.repository;

import com.minilands.backend.entity.Transaction;
import com.minilands.backend.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TransactionRepository extends MongoRepository<Transaction, String> {

    List<Transaction> findByWalletId(String walletId);

    List<Transaction> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Transaction> findByUserIdAndType(String userId, TransactionType type);

    Page<Transaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<Transaction> findByUserIdAndTypeOrderByCreatedAtDesc(String userId, TransactionType type, Pageable pageable);

    long countByUserId(String userId);

    long countByUserIdAndType(String userId, TransactionType type);
}
