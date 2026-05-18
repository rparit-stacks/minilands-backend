package com.minilands.backend.repository;

import com.minilands.backend.entity.Deposit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DepositRepository extends MongoRepository<Deposit, String> {

    List<Deposit> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Deposit> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Deposit> findByRazorpayPaymentId(String razorpayPaymentId);
}
