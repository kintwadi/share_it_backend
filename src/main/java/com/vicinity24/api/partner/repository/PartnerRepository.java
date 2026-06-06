package com.vicinity24.api.partner.repository;

import com.vicinity24.api.partner.model.Partner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PartnerRepository extends JpaRepository<Partner, UUID> {
}
