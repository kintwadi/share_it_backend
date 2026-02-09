package com.nearshare.api.repository;

import com.nearshare.api.model.Report;
import com.nearshare.api.model.Listing;
import com.nearshare.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Report r WHERE r.reporter.id = :reporterId AND r.listing.id = :listingId AND r.reason = :reason")
    boolean existsByReporterIdAndListingIdAndReason(@Param("reporterId") UUID reporterId, @Param("listingId") UUID listingId, @Param("reason") String reason);
}
