package com.vicinity24.api.bicycle.repository;

import com.vicinity24.api.bicycle.domain.model.BikeSpecAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BikeSpecAttributeRepository extends JpaRepository<BikeSpecAttribute, Long> {

    List<BikeSpecAttribute> findByTenantIdOrderByAttributeNameAsc(String tenantId);

    Optional<BikeSpecAttribute> findByTenantIdAndAttributeNameIgnoreCase(String tenantId, String attributeName);
}
