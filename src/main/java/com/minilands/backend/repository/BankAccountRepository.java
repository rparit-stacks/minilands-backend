package com.minilands.backend.repository;

import com.minilands.backend.entity.BankAccount;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends MongoRepository<BankAccount, String> {

    List<BankAccount> findByUserId(String userId);

    Optional<BankAccount> findByUserIdAndPrimaryTrue(String userId);
}
