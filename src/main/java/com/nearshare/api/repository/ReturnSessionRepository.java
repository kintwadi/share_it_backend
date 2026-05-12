package com.nearshare.api.repository;

import com.nearshare.api.model.ReturnSession;
import com.nearshare.api.model.enums.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReturnSessionRepository extends JpaRepository<ReturnSession, UUID> {
    void deleteByListingId(UUID listingId);
    List<ReturnSession> findByListingIdAndStatusOrderByCreatedAtDesc(UUID listingId, ReturnStatus status);
    Optional<ReturnSession> findFirstByListingIdAndStatusOrderByCreatedAtDesc(UUID listingId, ReturnStatus status);
    Optional<ReturnSession> findFirstByListingIdOrderByCreatedAtDesc(UUID listingId);
    List<ReturnSession> findByStatusOrderByCreatedAtDesc(ReturnStatus status);
    List<ReturnSession> findByStatusAndExpiresAtBefore(ReturnStatus status, java.time.LocalDateTime now);
    long countByStatus(ReturnStatus status);
}
