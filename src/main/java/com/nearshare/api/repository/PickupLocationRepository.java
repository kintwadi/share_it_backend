package com.nearshare.api.repository;

import com.nearshare.api.model.PickupLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PickupLocationRepository extends JpaRepository<PickupLocation, UUID> {
    List<PickupLocation> findByActiveTrue();
    Optional<PickupLocation> findByReferenceId(String referenceId);
    boolean existsByReferenceId(String referenceId);
}
