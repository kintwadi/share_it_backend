package com.vicinity24.api.bicycle.service;

import com.vicinity24.api.bicycle.dto.BicycleCatalogItemDto;
import com.vicinity24.api.bicycle.dto.BicycleCatalogPageDto;
import com.vicinity24.api.bicycle.dto.BicycleCatalogPaginationDto;
import com.vicinity24.api.bicycle.dto.BicycleDetailDto;
import com.vicinity24.api.bicycle.repository.BicycleCatalogRow;
import com.vicinity24.api.bicycle.repository.BicycleListingRepository;
import com.vicinity24.api.core.model.Listing;
import com.vicinity24.api.core.repository.ListingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BicycleCatalogService {

    private final BicycleListingRepository bicycleListingRepository;
    private final ListingRepository listingRepository;
    private final JdbcTemplate jdbcTemplate;

    public BicycleCatalogService(
            BicycleListingRepository bicycleListingRepository,
            ListingRepository listingRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.bicycleListingRepository = bicycleListingRepository;
        this.listingRepository = listingRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public BicycleCatalogPageDto search(
            String search,
            String city,
            String frameSize,
            String bikeType,
            String inventoryStatus,
            String sort,
            Pageable pageable
    ) {
        String normalizedSort = (sort == null || sort.isBlank()) ? "newest" : sort;
        Page<BicycleCatalogItemDto> page = bicycleListingRepository
                .searchCatalog(search, city, frameSize, bikeType, inventoryStatus, normalizedSort, pageable)
                .map(this::mapCatalogItem);

        return BicycleCatalogPageDto.builder()
                .content(page.getContent())
                .pagination(buildPagination(page))
                .build();
    }

    @Transactional(readOnly = true)
    public BicycleDetailDto getByListingId(UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("bike_listing_not_found"));
        BicycleDetailRow detail = jdbcTemplate.query(
                """
                select frame_size, bike_type, assembly_buffer_minutes, rent_to_own_eligible, retail_purchase_price, inventory_status
                from bicycle.bike_listings
                where listing_id = ?
                """,
                rs -> rs.next()
                        ? new BicycleDetailRow(
                        rs.getString("frame_size"),
                        rs.getString("bike_type"),
                        rs.getInt("assembly_buffer_minutes"),
                        rs.getBoolean("rent_to_own_eligible"),
                        rs.getBigDecimal("retail_purchase_price"),
                        rs.getString("inventory_status")
                )
                        : null,
                listingId
        );
        if (detail == null) {
            throw new RuntimeException("bike_listing_not_found");
        }
        return mapDetail(listing, detail);
    }

    private BicycleCatalogItemDto mapCatalogItem(BicycleCatalogRow row) {
        return BicycleCatalogItemDto.builder()
                .id(row.getId())
                .title(row.getTitle())
                .description(row.getDescription())
                .category(row.getCategory())
                .imageUrl(row.getImageUrl())
                .hourlyRate(row.getHourlyRate())
                .city(row.getCity())
                .country(row.getCountry())
                .frameSize(row.getFrameSize())
                .bikeType(row.getBikeType())
                .assemblyBufferMinutes(row.getAssemblyBufferMinutes())
                .rentToOwnEligible(Boolean.TRUE.equals(row.getRentToOwnEligible()))
                .retailPurchasePrice(row.getRetailPurchasePrice())
                .inventoryStatus(row.getInventoryStatus())
                .createdAt(row.getCreatedAt())
                .build();
    }

    private BicycleDetailDto mapDetail(Listing listing, BicycleDetailRow detail) {
        List<String> gallery = listing.getGallery() == null ? List.of() : List.copyOf(listing.getGallery());
        return BicycleDetailDto.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .imageUrl(listing.getImageUrl())
                .gallery(gallery)
                .category(listing.getCategory())
                .hourlyRate(listing.getHourlyRate())
                .city(listing.getCity())
                .country(listing.getCountry())
                .frameSize(detail.frameSize())
                .bikeType(detail.bikeType())
                .assemblyBufferMinutes(detail.assemblyBufferMinutes())
                .rentToOwnEligible(detail.rentToOwnEligible())
                .retailPurchasePrice(detail.retailPurchasePrice())
                .inventoryStatus(detail.inventoryStatus())
                .ownerName(listing.getOwner() != null ? listing.getOwner().getName() : null)
                .partnerName(listing.getPartner() != null ? listing.getPartner().getName() : null)
                .build();
    }

    private BicycleCatalogPaginationDto buildPagination(Page<BicycleCatalogItemDto> page) {
        int totalPages = Math.max(page.getTotalPages(), 1);
        int currentPage = Math.min(page.getNumber(), totalPages - 1);
        int maxVisiblePages = 10;
        int start = 0;
        int end = totalPages;

        if (totalPages > maxVisiblePages) {
            int halfWindow = maxVisiblePages / 2;
            start = Math.max(0, currentPage - halfWindow);
            end = start + maxVisiblePages;

            if (end > totalPages) {
                end = totalPages;
                start = end - maxVisiblePages;
            }
        }

        List<Integer> pages = java.util.stream.IntStream.range(start, end)
                .boxed()
                .toList();
        long from = page.getTotalElements() == 0 ? 0 : ((long) currentPage * page.getSize()) + 1;
        long to = page.getTotalElements() == 0 ? 0 : Math.min(from + page.getNumberOfElements() - 1, page.getTotalElements());

        return BicycleCatalogPaginationDto.builder()
                .currentPage(currentPage)
                .pageSize(page.getSize())
                .totalPages(totalPages)
                .totalElements(page.getTotalElements())
                .from(from)
                .to(to)
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .pages(pages)
                .build();
    }

    private record BicycleDetailRow(
            String frameSize,
            String bikeType,
            Integer assemblyBufferMinutes,
            boolean rentToOwnEligible,
            java.math.BigDecimal retailPurchasePrice,
            String inventoryStatus
    ) {
    }
}
