package com.nearshare.api.partner.repository;

import com.nearshare.api.partner.model.Partner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PartnerRepository extends JpaRepository<Partner, UUID> {
}
