package com.vicinity24.api.bicycle.repository;

import com.vicinity24.api.bicycle.domain.model.BikeSpecValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BikeSpecValueRepository extends JpaRepository<BikeSpecValue, Long> {

    List<BikeSpecValue> findByAttributeIdIn(Collection<Long> attributeIds);

    List<BikeSpecValue> findByAttributeIdOrderByValueTextAsc(Long attributeId);

    Optional<BikeSpecValue> findByAttributeIdAndValueTextIgnoreCase(Long attributeId, String valueText);
}
