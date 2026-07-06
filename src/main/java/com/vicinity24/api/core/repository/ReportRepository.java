package com.vicinity24.api.core.repository;

import com.vicinity24.api.core.model.Report;
import com.vicinity24.api.core.model.Listing;
import com.vicinity24.api.core.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Report r WHERE r.reporter.id = :reporterId AND r.listing.id = :listingId AND r.reason = :reason")
    boolean existsByReporterIdAndListingIdAndReason(@Param("reporterId") UUID reporterId, @Param("listingId") UUID listingId, @Param("reason") String reason);

    List<Report> findByListing(Listing listing);
    List<Report> findByReporter(User reporter);
}
