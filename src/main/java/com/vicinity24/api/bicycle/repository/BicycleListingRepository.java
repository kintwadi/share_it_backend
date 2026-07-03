package com.vicinity24.api.bicycle.repository;

import com.vicinity24.api.bicycle.domain.model.BicycleListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BicycleListingRepository extends JpaRepository<BicycleListing, UUID> {

    @Query(
            value = """
                    select
                        l.id as id,
                        l.title as title,
                        l.description as description,
                        l.category as category,
                        l.image_url as imageUrl,
                        l.hourly_rate as hourlyRate,
                        l.city as city,
                        l.country as country,
                        b.frame_size as frameSize,
                        b.bike_type as bikeType,
                        b.assembly_buffer_minutes as assemblyBufferMinutes,
                        b.rent_to_own_eligible as rentToOwnEligible,
                        b.retail_purchase_price as retailPurchasePrice,
                        b.inventory_status as inventoryStatus,
                        l.created_at as createdAt
                    from public.listings l
                    join bicycle.bike_listings b on b.listing_id = l.id
                    where (l.status is null or l.status not in ('BLOCKED', 'HIDDEN', 'PARTNER_INACTIVE'))
                      and (:search is null or trim(:search) = ''
                           or lower(coalesce(l.title, '')) like lower(concat('%', :search, '%'))
                           or lower(coalesce(l.description, '')) like lower(concat('%', :search, '%')))
                      and (:city is null or trim(:city) = '' or lower(coalesce(l.city, '')) like lower(concat('%', :city, '%')))
                      and (:frameSize is null or trim(:frameSize) = '' or lower(coalesce(b.frame_size, '')) = lower(:frameSize))
                      and (:bikeType is null or trim(:bikeType) = '' or lower(cast(b.bike_type as varchar)) = lower(:bikeType))
                      and (:inventoryStatus is null or trim(:inventoryStatus) = '' or lower(cast(b.inventory_status as varchar)) = lower(:inventoryStatus))
                    order by
                      case when :sort = 'priceAsc' then b.retail_purchase_price end asc nulls last,
                      case when :sort = 'priceDesc' then b.retail_purchase_price end desc nulls last,
                      case when :sort = 'titleAsc' then l.title end asc nulls last,
                      case when :sort = 'titleDesc' then l.title end desc nulls last,
                      case when :sort = 'oldest' then l.created_at end asc nulls last,
                      l.created_at desc nulls last,
                      l.id desc
                    """,
            countQuery = """
                    select count(*)
                    from public.listings l
                    join bicycle.bike_listings b on b.listing_id = l.id
                    where (l.status is null or l.status not in ('BLOCKED', 'HIDDEN', 'PARTNER_INACTIVE'))
                      and (:search is null or trim(:search) = ''
                           or lower(coalesce(l.title, '')) like lower(concat('%', :search, '%'))
                           or lower(coalesce(l.description, '')) like lower(concat('%', :search, '%')))
                      and (:city is null or trim(:city) = '' or lower(coalesce(l.city, '')) like lower(concat('%', :city, '%')))
                      and (:frameSize is null or trim(:frameSize) = '' or lower(coalesce(b.frame_size, '')) = lower(:frameSize))
                      and (:bikeType is null or trim(:bikeType) = '' or lower(cast(b.bike_type as varchar)) = lower(:bikeType))
                      and (:inventoryStatus is null or trim(:inventoryStatus) = '' or lower(cast(b.inventory_status as varchar)) = lower(:inventoryStatus))
                    """,
            nativeQuery = true
    )
    Page<BicycleCatalogRow> searchCatalog(
            @Param("search") String search,
            @Param("city") String city,
            @Param("frameSize") String frameSize,
            @Param("bikeType") String bikeType,
            @Param("inventoryStatus") String inventoryStatus,
            @Param("sort") String sort,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"coreListing", "coreListing.owner", "coreListing.borrower", "coreListing.partner"})
    Optional<BicycleListing> findWithCoreListingByListingId(UUID listingId);
}
