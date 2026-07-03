package com.vicinity24.api.bicycle.repository;

import com.vicinity24.api.bicycle.domain.model.Bike;
import com.vicinity24.api.bicycle.domain.valueobject.BikeSaleType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface BikeRepository extends JpaRepository<Bike, Long>, JpaSpecificationExecutor<Bike> {

    Optional<Bike> findByTenantIdAndBrandNameIgnoreCaseAndModelNameIgnoreCaseAndModelYearAndSaleType(
            String tenantId,
            String brandName,
            String modelName,
            Integer modelYear,
            BikeSaleType saleType
    );

    boolean existsByTenantId(String tenantId);

    @EntityGraph(attributePaths = {"specMappings", "specMappings.specValue", "specMappings.specValue.attribute"})
    Optional<Bike> findWithSkusAndSpecsByIdAndTenantId(Long id, String tenantId);
}
