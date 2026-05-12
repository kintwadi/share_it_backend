package com.nearshare.api.repository;

import com.nearshare.api.model.Listing;
import com.nearshare.api.model.User;
import com.nearshare.api.model.enums.AvailabilityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID> {
    List<Listing> findByTitle(String title);
    List<Listing> findByOwner(User owner);
    List<Listing> findByBorrower(User borrower);
    List<Listing> findByCategoryAndTitleContainingIgnoreCase(String category, String titleKeyword);
    List<Listing> findByStatusOrderByCreatedAtDesc(AvailabilityStatus status);
    Page<Listing> findByStatus(AvailabilityStatus status, Pageable pageable);
    long countByStatus(AvailabilityStatus status);
}
