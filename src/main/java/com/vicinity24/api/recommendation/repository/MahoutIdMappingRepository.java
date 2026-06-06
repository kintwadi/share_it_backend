package com.vicinity24.api.recommendation.repository;

import com.vicinity24.api.recommendation.model.MahoutIdMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MahoutIdMappingRepository extends JpaRepository<MahoutIdMapping, Long> {
    Optional<MahoutIdMapping> findByEntityIdAndEntityType(UUID entityId, String entityType);
    Optional<MahoutIdMapping> findByMahoutIdAndEntityType(Long mahoutId, String entityType);
    
    // Find max mahoutId to increment safely
    MahoutIdMapping findTopByOrderByMahoutIdDesc();
}
