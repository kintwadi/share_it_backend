package com.vicinity24.api.bicycle.service;

import com.vicinity24.api.bicycle.domain.model.FahradFuchsBooking;
import com.vicinity24.api.bicycle.dto.FahradFuchsBookingDto;
import com.vicinity24.api.bicycle.dto.FahradFuchsCatalogItemDto;
import com.vicinity24.api.bicycle.dto.FahradFuchsCheckoutRequest;
import com.vicinity24.api.bicycle.dto.FahradFuchsCheckoutResponse;
import com.vicinity24.api.bicycle.dto.FahradFuchsFrameOptionDto;
import com.vicinity24.api.bicycle.dto.FahradFuchsListingDetailDto;
import com.vicinity24.api.bicycle.dto.FahradFuchsStoreDto;
import com.vicinity24.api.bicycle.dto.FahradFuchsStorefrontDto;
import com.vicinity24.api.bicycle.repository.FahradFuchsBookingRepository;
import com.vicinity24.api.core.model.Listing;
import com.vicinity24.api.core.model.Transaction;
import com.vicinity24.api.core.model.User;
import com.vicinity24.api.core.repository.ListingRepository;
import com.vicinity24.api.core.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class FahradFuchsStorefrontService {

    private final JdbcTemplate jdbcTemplate;
    private final ListingRepository listingRepository;
    private final TransactionRepository transactionRepository;
    private final FahradFuchsBookingRepository bookingRepository;
    private final CorePaymentGateway corePaymentGateway;
    private final Map<String, FahradFuchsCatalogDefinitions.FahradFuchsBikeDefinition> definitionsBySlug;

    public FahradFuchsStorefrontService(
            JdbcTemplate jdbcTemplate,
            ListingRepository listingRepository,
            TransactionRepository transactionRepository,
            FahradFuchsBookingRepository bookingRepository,
            CorePaymentGateway corePaymentGateway
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.listingRepository = listingRepository;
        this.transactionRepository = transactionRepository;
        this.bookingRepository = bookingRepository;
        this.corePaymentGateway = corePaymentGateway;
        this.definitionsBySlug = FahradFuchsCatalogDefinitions.bySlug();
    }

    @Transactional(readOnly = true)
    public FahradFuchsStorefrontDto storefront() {
        return new FahradFuchsStorefrontDto(store(), listBikes());
    }

    @Transactional(readOnly = true)
    public List<FahradFuchsCatalogItemDto> listBikes() {
        return jdbcTemplate.query(
                """
                select c.slug,
                       c.display_order,
                       l.id as listing_id,
                       l.title,
                       l.description,
                       l.image_url,
                       l.hourly_rate,
                       bl.retail_purchase_price
                from bicycle.fahrad_fuchs_catalog c
                join public.listings l on l.id = c.listing_id
                left join bicycle.bike_listings bl on bl.listing_id = l.id
                order by c.display_order asc
                """,
                (rs, rowNum) -> {
                    String slug = rs.getString("slug");
                    FahradFuchsCatalogDefinitions.FahradFuchsBikeDefinition definition = requireDefinition(slug);
                    BigDecimal dailyRate = rs.getBigDecimal("hourly_rate");
                    BigDecimal retailPrice = rs.getBigDecimal("retail_purchase_price");
                    return new FahradFuchsCatalogItemDto(
                            (UUID) rs.getObject("listing_id"),
                            slug,
                            rs.getString("title"),
                            definition.category(),
                            definition.teaser(),
                            FahradFuchsCatalogDefinitions.AVAILABILITY_BADGE,
                            dailyRate,
                            retailPrice,
                            rs.getString("image_url"),
                            definition.valuePoints()
                    );
                }
        );
    }

    @Transactional(readOnly = true)
    public FahradFuchsListingDetailDto getBike(String slug) {
        FahradFuchsCatalogDefinitions.FahradFuchsBikeDefinition definition = requireDefinition(slug);
        Listing listing = getListingBySlug(slug);
        BigDecimal retailPrice = jdbcTemplate.query(
                "select retail_purchase_price from bicycle.bike_listings where listing_id = ?",
                rs -> rs.next() ? rs.getBigDecimal("retail_purchase_price") : definition.retailPrice(),
                listing.getId()
        );

        return new FahradFuchsListingDetailDto(
                listing.getId(),
                slug,
                listing.getTitle(),
                definition.category(),
                FahradFuchsCatalogDefinitions.AVAILABILITY_BADGE,
                listing.getDescription(),
                listing.getHourlyRate(),
                retailPrice,
                listing.getImageUrl(),
                listing.getGallery() == null || listing.getGallery().isEmpty() ? definition.gallery() : List.copyOf(listing.getGallery()),
                definition.valuePoints(),
                definition.technicalSpecs(),
                definition.frameOptions(),
                store()
        );
    }

    @Transactional
    public FahradFuchsCheckoutResponse checkout(String slug, FahradFuchsCheckoutRequest request, User borrower) {
        Listing listing = getListingBySlug(slug);
        FahradFuchsCatalogDefinitions.FahradFuchsBikeDefinition definition = requireDefinition(slug);

        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_date_range");
        }

        List<FahradFuchsFrameOptionDto> frameOptions = definition.frameOptions();
        boolean validFrame = frameOptions.stream().anyMatch(option -> option.value().equalsIgnoreCase(request.frameSizeOption()));
        if (!validFrame) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_frame_size_option");
        }

        long reservationDays = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        if (reservationDays <= 0) {
            reservationDays = 1;
        }

        BigDecimal totalAmount = listing.getHourlyRate()
                .multiply(BigDecimal.valueOf(reservationDays))
                .setScale(2, RoundingMode.HALF_UP);
        String paymentMethod = normalizePaymentMethod(request.paymentMethod());
        String paymentToken = normalizePaymentToken(request.paymentToken());

        boolean paid = corePaymentGateway.executeCharge(totalAmount, "EUR", paymentMethod, paymentToken);
        if (!paid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payment_failed");
        }

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .payer(borrower)
                .payee(listing.getOwner())
                .amount(totalAmount)
                .rentalAmount(totalAmount)
                .serviceFeeAmount(BigDecimal.ZERO)
                .depositAmount(BigDecimal.ZERO)
                .currency("EUR")
                .paymentMethod(paymentMethod)
                .paymentToken(paymentToken)
                .borrowerPath("NA")
                .timestamp(LocalDateTime.now())
                .status("ESCROWED")
                .build();
        transactionRepository.save(transaction);

        FahradFuchsBooking booking = FahradFuchsBooking.builder()
                .id(UUID.randomUUID())
                .bookingReference(nextBookingReference())
                .listing(listing)
                .borrower(borrower)
                .bikeSlug(slug)
                .bikeTitle(listing.getTitle())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .frameSizeOption(resolveFrameLabel(frameOptions, request.frameSizeOption()))
                .totalAmount(totalAmount)
                .currency("EUR")
                .paymentMethod(paymentMethod)
                .paymentToken(paymentToken)
                .status("CONFIRMED")
                .createdAt(LocalDateTime.now())
                .build();
        bookingRepository.save(booking);

        return toCheckoutResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<FahradFuchsBookingDto> getBookings(User borrower) {
        return bookingRepository.findByBorrowerIdOrderByCreatedAtDesc(borrower.getId()).stream()
                .map(this::toBookingDto)
                .toList();
    }

    private FahradFuchsCheckoutResponse toCheckoutResponse(FahradFuchsBooking booking) {
        return new FahradFuchsCheckoutResponse(
                booking.getId(),
                booking.getBookingReference(),
                booking.getListing().getId(),
                booking.getBikeSlug(),
                booking.getBikeTitle(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getFrameSizeOption(),
                booking.getTotalAmount(),
                booking.getCurrency(),
                booking.getPaymentMethod(),
                booking.getStatus(),
                store()
        );
    }

    private FahradFuchsBookingDto toBookingDto(FahradFuchsBooking booking) {
        return new FahradFuchsBookingDto(
                booking.getId(),
                booking.getBookingReference(),
                booking.getListing().getId(),
                booking.getBikeSlug(),
                booking.getBikeTitle(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getFrameSizeOption(),
                booking.getTotalAmount(),
                booking.getCurrency(),
                booking.getStatus(),
                booking.getListing().getImageUrl(),
                booking.getCreatedAt()
        );
    }

    private String normalizePaymentMethod(String paymentMethod) {
        return paymentMethod == null || paymentMethod.isBlank()
                ? "PAYPAL"
                : paymentMethod.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizePaymentToken(String paymentToken) {
        if (paymentToken != null && !paymentToken.isBlank()) {
            return paymentToken.trim();
        }
        return "fahrad-fuchs-demo-" + UUID.randomUUID();
    }

    private String resolveFrameLabel(List<FahradFuchsFrameOptionDto> options, String value) {
        return options.stream()
                .filter(option -> option.value().equalsIgnoreCase(value))
                .map(FahradFuchsFrameOptionDto::label)
                .findFirst()
                .orElse(value);
    }

    private String nextBookingReference() {
        while (true) {
            String candidate = "FF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            if (!bookingRepository.existsByBookingReference(candidate)) {
                return candidate;
            }
        }
    }

    private Listing getListingBySlug(String slug) {
        UUID listingId = jdbcTemplate.query(
                "select listing_id from bicycle.fahrad_fuchs_catalog where slug = ?",
                rs -> rs.next() ? (UUID) rs.getObject("listing_id") : null,
                slug
        );
        if (listingId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "fahrad_fuchs_bike_not_found");
        }
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "fahrad_fuchs_bike_not_found"));
    }

    private FahradFuchsCatalogDefinitions.FahradFuchsBikeDefinition requireDefinition(String slug) {
        FahradFuchsCatalogDefinitions.FahradFuchsBikeDefinition definition = definitionsBySlug.get(slug);
        if (definition == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "fahrad_fuchs_bike_not_found");
        }
        return definition;
    }

    private FahradFuchsStoreDto store() {
        return FahradFuchsCatalogDefinitions.store();
    }
}
