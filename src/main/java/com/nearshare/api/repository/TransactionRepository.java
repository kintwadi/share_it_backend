package com.nearshare.api.repository;

import com.nearshare.api.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByPayerId(UUID payerId);
    @EntityGraph(attributePaths = {"listing", "payer", "payee"})
    List<Transaction> findByPayerIdOrderByTimestampDesc(UUID payerId);
    List<Transaction> findByPayeeId(UUID payeeId);
    @EntityGraph(attributePaths = {"listing", "payer", "payee"})
    List<Transaction> findByPayeeIdOrderByTimestampDesc(UUID payeeId);
    List<Transaction> findByPayeeIdAndStatusOrderByTimestampDesc(UUID payeeId, String status);
    List<Transaction> findByListingId(UUID listingId);
    java.util.Optional<Transaction> findFirstByListingIdOrderByTimestampDesc(UUID listingId);
    java.util.Optional<Transaction> findTopByListingIdAndStatusOrderByTimestampDesc(UUID listingId, String status);
    java.util.Optional<Transaction> findByPaymentToken(String paymentToken);
    long countByStatus(String status);
    Page<Transaction> findByStatus(String status, Pageable pageable);
}
