package com.vicinity24.api.core.partner.repository;

import com.vicinity24.api.core.partner.model.PartnerSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PartnerSettingsRepository extends JpaRepository<PartnerSettings, UUID> {
    @Query("select ps from PartnerSettings ps where ps.partner.id = :partnerId")
    Optional<PartnerSettings> findByPartnerId(@Param("partnerId") UUID partnerId);
}
