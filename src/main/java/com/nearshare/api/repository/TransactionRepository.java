package com.nearshare.api.repository;

import com.nearshare.api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByPayerId(UUID payerId);
    List<Transaction> findByPayeeId(UUID payeeId);
    List<Transaction> findByListingId(UUID listingId);
    java.util.Optional<Transaction> findByPaymentToken(String paymentToken);
}
