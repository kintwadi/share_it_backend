package com.nearshare.api.repository;

import com.nearshare.api.model.Listing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID> {
}