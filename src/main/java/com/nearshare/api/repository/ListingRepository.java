package com.nearshare.api.repository;

import com.nearshare.api.model.Listing;
import com.nearshare.api.model.User;
import com.nearshare.api.model.enums.AvailabilityStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID> {
    List<Listing> findByTitle(String title);
    List<Listing> findByOwner(User owner);
    List<Listing> findByBorrower(User borrower);
    List<Listing> findByCategoryAndTitleContainingIgnoreCase(String category, String titleKeyword);
    List<Listing> findByStatusOrderByCreatedAtDesc(AvailabilityStatus status);
    Page<Listing> findByStatus(AvailabilityStatus status, Pageable pageable);
    Page<Listing> findByPartnerIsNull(Pageable pageable);
    Page<Listing> findByPartnerIsNullAndStatus(AvailabilityStatus status, Pageable pageable);
    Page<Listing> findByPartnerIsNotNull(Pageable pageable);
    Page<Listing> findByPartnerIsNotNullAndStatus(AvailabilityStatus status, Pageable pageable);
    Page<Listing> findByPartnerIsNotNullAndStatusIn(Collection<AvailabilityStatus> statuses, Pageable pageable);
    Page<Listing> findByPartnerIdIn(Set<UUID> partnerIds, Pageable pageable);
    Page<Listing> findByPartnerIdInAndStatus(Set<UUID> partnerIds, AvailabilityStatus status, Pageable pageable);
    Page<Listing> findByPartnerIdInAndStatusIn(Set<UUID> partnerIds, Collection<AvailabilityStatus> statuses, Pageable pageable);
    boolean existsByItemReference(String itemReference);
    long countByStatus(AvailabilityStatus status);

    interface ListingDistanceRow {
        UUID getId();
        Double getDistanceKm();
    }

    @Query(value = """
            select t.id as id, t.distance_km as distanceKm
            from (
                select l.id as id,
                       (6371 * acos(
                           cos(radians(:borrowerLat)) * cos(radians(l.lat)) *
                           cos(radians(l.lng) - radians(:borrowerLng)) +
                           sin(radians(:borrowerLat)) * sin(radians(l.lat))
                       )) as distance_km
                from listings l
                where l.status = 'AVAILABLE'
                  and (l.available_unlimited = true or l.available_from is null or l.available_from <= CURRENT_TIMESTAMP)
                  and (l.available_to is null or l.available_to >= CURRENT_TIMESTAMP)
                  and l.lat is not null
                  and l.lng is not null
            ) t
            where t.distance_km <= :radiusKm
            order by t.distance_km asc
            """, nativeQuery = true)
    List<ListingDistanceRow> findNearby(
            @Param("borrowerLat") double borrowerLat,
            @Param("borrowerLng") double borrowerLng,
            @Param("radiusKm") double radiusKm,
            Pageable pageable
    );
}
