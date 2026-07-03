package com.vicinity24.api.bicycle.repository;

import com.vicinity24.api.bicycle.domain.model.BikeSku;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BikeSkuRepository extends JpaRepository<BikeSku, Long> {
}
