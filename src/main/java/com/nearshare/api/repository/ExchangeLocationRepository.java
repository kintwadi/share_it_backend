package com.nearshare.api.repository;

import com.nearshare.api.model.ExchangeLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeLocationRepository extends JpaRepository<ExchangeLocation, UUID> {
    List<ExchangeLocation> findByActiveTrue();
    Optional<ExchangeLocation> findByReferenceId(String referenceId);
    boolean existsByReferenceId(String referenceId);
}

