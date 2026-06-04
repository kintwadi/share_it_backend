package com.nearshare.api.service;

import com.nearshare.api.dto.CreateListingRequest;
import com.nearshare.api.dto.ListingDTO;
import com.nearshare.api.dto.LocationDTO;
import com.nearshare.api.dto.ExchangeLocationDTO;
import com.nearshare.api.dto.UserSummaryDTO;
import com.nearshare.api.config.RuntimeSettingsService;
import com.nearshare.api.model.Listing;
import com.nearshare.api.model.User;
import com.nearshare.api.model.Report;
import com.nearshare.api.model.embeddable.Location;
import com.nearshare.api.model.enums.AvailabilityStatus;
import com.nearshare.api.model.enums.ListingType;
import com.nearshare.api.model.enums.UserRole;
import com.nearshare.api.repository.ListingRepository;
import com.nearshare.api.repository.ExchangeLocationRepository;
import com.nearshare.api.repository.ReturnSessionRepository;
import com.nearshare.api.repository.UserRepository;
import com.nearshare.api.repository.ReportRepository;
import com.nearshare.api.repository.ReviewRepository;
import com.nearshare.api.util.DistanceUtil;
import com.nearshare.api.util.GeohashUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
public class ListingService {
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ExchangeLocationRepository exchangeLocationRepository;
    private final com.nearshare.api.repository.RecommendationDismissRepository dismissRepository;
    private final com.nearshare.api.payment.PaymentManager paymentManager;
    private final com.nearshare.api.repository.TransactionRepository transactionRepository;
    private final ReturnSessionRepository returnSessionRepository;
    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final SubscriptionService subscriptionService;
    private final TrustScoreService trustScoreService;
    private final RuntimeSettingsService runtimeSettingsService;
    private final LocationService locationService;
    private final MessageService messageService;
    private final EmailService emailService;
    private final SecureRandom itemRefRandom = new SecureRandom();

    public ListingService(
            ListingRepository listingRepository,
            UserRepository userRepository,
            ExchangeLocationRepository exchangeLocationRepository,
            com.nearshare.api.repository.RecommendationDismissRepository dismissRepository,
            com.nearshare.api.payment.PaymentManager paymentManager,
            com.nearshare.api.repository.TransactionRepository transactionRepository,
            ReturnSessionRepository returnSessionRepository,
            ReportRepository reportRepository,
            ReviewRepository reviewRepository,
            SubscriptionService subscriptionService,
            TrustScoreService trustScoreService,
            RuntimeSettingsService runtimeSettingsService,
            LocationService locationService,
            MessageService messageService,
            EmailService emailService) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.exchangeLocationRepository = exchangeLocationRepository;
        this.dismissRepository = dismissRepository;
        this.paymentManager = paymentManager;
        this.transactionRepository = transactionRepository;
        this.returnSessionRepository = returnSessionRepository;
        this.reportRepository = reportRepository;
        this.reviewRepository = reviewRepository;
        this.subscriptionService = subscriptionService;
        this.trustScoreService = trustScoreService;
        this.runtimeSettingsService = runtimeSettingsService;
        this.locationService = locationService;
        this.messageService = messageService;
        this.emailService = emailService;
    }

    private boolean isSellEnabled() {
        return runtimeSettingsService != null && runtimeSettingsService.isEnabled("settings.enable.sell", false);
    }

    @Transactional(readOnly = true)
    public Page<ListingDTO> findAll(User current, String search, String category, String type, Double minPrice, int page, int size) {
        return findAll(current, search, category, type, minPrice, page, size, null, null);
    }

    @Transactional(readOnly = true)
    public Page<ListingDTO> findAll(User current, String search, String category, String type, Double minPrice, int page, int size, Double viewerLat, Double viewerLng) {
        List<Listing> all = listingRepository.findAll();
        List<Listing> filtered = all.stream()
            .filter(l -> l.getStatus() == null || (l.getStatus() != AvailabilityStatus.BLOCKED && l.getStatus() != AvailabilityStatus.HIDDEN))
            .filter(l -> !(l.getPartner() != null && l.getStatus() == AvailabilityStatus.PARTNER_INACTIVE))
            .filter(l -> isAvailableForDiscovery(current, l))
            .filter(l -> {
                if (l.getPartner() == null || l.getBorrower() == null) return true;
                boolean isAdmin = current != null && current.getRole() == UserRole.ADMIN;
                boolean isBorrower = current != null && current.getId() != null && l.getBorrower() != null && current.getId().equals(l.getBorrower().getId());
                return isAdmin || isBorrower;
            })
            .filter(l -> search == null || (l.getTitle() != null && l.getTitle().toLowerCase().contains(search.toLowerCase())))
            .filter(l -> category == null || (l.getCategory() != null && l.getCategory().equalsIgnoreCase(category)))
            .filter(l -> type == null || (l.getType() != null && l.getType().name().equalsIgnoreCase(type)))
            .filter(l -> minPrice == null || (l.getHourlyRate() != null && l.getHourlyRate().compareTo(BigDecimal.valueOf(minPrice)) >= 0))
            .toList();

        if (viewerLat != null && viewerLng != null) {
            filtered = filtered.stream()
                    .sorted((a, b) -> Double.compare(distanceForSort(viewerLat, viewerLng, a), distanceForSort(viewerLat, viewerLng, b)))
                    .toList();
        }
        int start = Math.min(page * size, filtered.size());
        int end = Math.min(start + size, filtered.size());
        List<ListingDTO> content = filtered.subList(start, end).stream().map(l -> toDTO(l, current, viewerLat, viewerLng)).toList();
        return new PageImpl<>(content, PageRequest.of(page, size), filtered.size());
    }

    @Transactional(readOnly = true)
    public ListingDTO getById(UUID id, User current) {
        Listing l = listingRepository.findById(id).orElseThrow(() -> new RuntimeException("listing_not_found"));
        if (!isAvailableForDiscovery(current, l)) {
            throw new RuntimeException("listing_not_found");
        }
        if (l.getPartner() != null) {
            boolean isAdmin = current != null && current.getRole() == UserRole.ADMIN;
            if (l.getStatus() == AvailabilityStatus.PARTNER_INACTIVE && !isAdmin) {
                throw new RuntimeException("listing_not_found");
            }
            if (l.getBorrower() != null && !isAdmin) {
                boolean isBorrower = current != null && current.getId() != null && l.getBorrower() != null && current.getId().equals(l.getBorrower().getId());
                if (!isBorrower) {
                    throw new RuntimeException("listing_not_found");
                }
            }
        }
        return toDTO(l, current);
    }

    @Transactional
    public ListingDTO create(User owner, CreateListingRequest req) {
        return create(owner, req, null, null);
    }

    @Transactional
    public ListingDTO create(User owner, CreateListingRequest req, Double viewerLat, Double viewerLng) {
        if (req.getType() == ListingType.SELL && !isSellEnabled()) {
            throw new RuntimeException("selling_disabled");
        }
        // Enforce subscription tiers
        if (req.getType() == ListingType.LEND) {
            if (!subscriptionService.isLenderPlan(owner)) {
                throw new RuntimeException("subscription_required_for_lending");
            }
        } else if (req.getType() == ListingType.SELL) {
            if (!subscriptionService.isProSeller(owner)) {
                throw new RuntimeException("subscription_required_for_selling");
            }
        }

        com.nearshare.api.model.ExchangeLocation pickup = null;
        if (req.getPickupLocationId() != null) {
            pickup = exchangeLocationRepository.findById(req.getPickupLocationId())
                    .orElseThrow(() -> new RuntimeException("pickup_location_not_found"));
        }
        String pickupStreet = req.getPickupLocationId() == null ? safePickupStreet(req.getPickupLocationStreet()) : null;
        String pickupHouse = req.getPickupLocationId() == null ? safePickupHouseNumber(req.getPickupLocationHouseNumber()) : null;
        String pickupCity = req.getPickupLocationId() == null ? safePickupCity(req.getPickupLocationCity()) : null;
        String pickupZip = req.getPickupLocationId() == null ? safePickupZip(req.getPickupLocationZip()) : null;
        String pickupCustom = req.getPickupLocationId() == null ? formatPickupCustom(pickupStreet, pickupHouse, pickupCity, pickupZip, safePickupCustom(req.getPickupLocationCustom())) : null;
        boolean autoApprove = subscriptionService.isPremiumLender(owner);
        BigDecimal hourlyRate = req.getHourlyRate();
        if (req.getType() == ListingType.GIVE) {
            hourlyRate = BigDecimal.ZERO;
        }
        String streetAddress = safeText(req.getStreetAddress());
        String city = safeText(req.getCity());
        String postalCode = safeText(req.getPostalCode());
        String country = safeText(req.getCountry());

        Double lat = req.getX();
        Double lng = req.getY();
        if ((!streetAddress.isEmpty() || !city.isEmpty() || !postalCode.isEmpty() || !country.isEmpty()) && locationService != null) {
            var geo = locationService.forwardGeocode(streetAddress, city, postalCode, country);
            if (geo != null) {
                lat = geo.getLatitude();
                lng = geo.getLongitude();
                streetAddress = safeText(geo.getStreetAddress());
                city = safeText(geo.getCity());
                postalCode = safeText(geo.getPostalCode());
                country = safeText(geo.getCountry());
            }
        }
        if (lat != null && lng != null && Math.abs(lat) < 1e-9 && Math.abs(lng) < 1e-9) {
            lat = null;
            lng = null;
        }
        if (lat == null || lng == null) {
            if (viewerLat != null && viewerLng != null) {
                lat = viewerLat;
                lng = viewerLng;
            } else if (owner != null && owner.getLocation() != null && owner.getLocation().getLat() != null && owner.getLocation().getLng() != null) {
                lat = owner.getLocation().getLat();
                lng = owner.getLocation().getLng();
            }
        }
        String geohash = GeohashUtil.encode(lat, lng, 9);
        boolean availableUnlimited = req.isAvailableUnlimited();
        java.time.LocalDateTime availableFrom = availableUnlimited ? null : req.getAvailableFrom();
        java.time.LocalDateTime availableTo = null;
        Listing l = Listing.builder()
                .id(UUID.randomUUID())
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .type(req.getType())
                .imageUrl(req.getImageUrl())
                .gallery(req.getGallery())
                .hourlyRate(hourlyRate)
                .autoApprove(autoApprove)
                .insuranceRequired(req.isInsuranceRequired())
                .status(AvailabilityStatus.AVAILABLE)
                .location(Location.builder().lat(lat).lng(lng).build())
                .streetAddress(streetAddress.isEmpty() ? null : streetAddress)
                .city(city.isEmpty() ? null : city)
                .postalCode(postalCode.isEmpty() ? null : postalCode)
                .country(country.isEmpty() ? null : country)
                .geohash(geohash)
                .owner(owner)
                .borrower(null)
                .pickupLocation(pickup)
                .pickupLocationCustom(pickupCustom)
                .pickupLocationStreet(pickupStreet)
                .pickupLocationHouseNumber(pickupHouse)
                .pickupLocationCity(pickupCity)
                .pickupLocationZip(pickupZip)
                .availableUnlimited(availableUnlimited)
                .availableFrom(availableFrom)
                .availableTo(availableTo)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        listingRepository.save(l);
        return toDTO(l, owner);
    }

    @Transactional
    public ListingDTO update(UUID id, CreateListingRequest req, User current) {
        Listing l = listingRepository.findById(id).orElseThrow(() -> new RuntimeException("listing_not_found"));
        if (l.getPartner() != null) {
            throw new RuntimeException("forbidden");
        }
        if (req.getType() == ListingType.SELL && !isSellEnabled()) {
            throw new RuntimeException("selling_disabled");
        }
        
        // Enforce subscription tiers
        if (req.getType() == ListingType.LEND) {
            if (!subscriptionService.isLenderPlan(current)) {
                throw new RuntimeException("subscription_required_for_lending");
            }
        } else if (req.getType() == ListingType.SELL) {
            if (!subscriptionService.isProSeller(current)) {
                throw new RuntimeException("subscription_required_for_selling");
            }
        }
        
        l.setTitle(req.getTitle());
        l.setDescription(req.getDescription());
        l.setCategory(req.getCategory());
        l.setType(req.getType());
        l.setImageUrl(req.getImageUrl());
        l.setGallery(req.getGallery());
        l.setInsuranceRequired(req.isInsuranceRequired());
        if (req.getType() == ListingType.GIVE) {
            l.setHourlyRate(BigDecimal.ZERO);
        } else {
            l.setHourlyRate(req.getHourlyRate());
        }
        l.setAutoApprove(subscriptionService.isPremiumLender(current));
        String nextStreetAddress = safeText(req.getStreetAddress());
        String nextCity = safeText(req.getCity());
        String nextPostalCode = safeText(req.getPostalCode());
        String nextCountry = safeText(req.getCountry());

        Double nextLat = req.getX();
        Double nextLng = req.getY();
        if ((!nextStreetAddress.isEmpty() || !nextCity.isEmpty() || !nextPostalCode.isEmpty() || !nextCountry.isEmpty()) && locationService != null) {
            var geo = locationService.forwardGeocode(nextStreetAddress, nextCity, nextPostalCode, nextCountry);
            if (geo != null) {
                nextLat = geo.getLatitude();
                nextLng = geo.getLongitude();
                nextStreetAddress = safeText(geo.getStreetAddress());
                nextCity = safeText(geo.getCity());
                nextPostalCode = safeText(geo.getPostalCode());
                nextCountry = safeText(geo.getCountry());
            }
        }
        if (nextLat != null && nextLng != null && Math.abs(nextLat) < 1e-9 && Math.abs(nextLng) < 1e-9) {
            nextLat = null;
            nextLng = null;
        }
        if (nextLat == null || nextLng == null) {
            if (l.getLocation() != null && l.getLocation().getLat() != null && l.getLocation().getLng() != null) {
                nextLat = l.getLocation().getLat();
                nextLng = l.getLocation().getLng();
            }
        }
        l.setLocation(Location.builder().lat(nextLat).lng(nextLng).build());
        l.setStreetAddress(nextStreetAddress.isEmpty() ? null : nextStreetAddress);
        l.setCity(nextCity.isEmpty() ? null : nextCity);
        l.setPostalCode(nextPostalCode.isEmpty() ? null : nextPostalCode);
        l.setCountry(nextCountry.isEmpty() ? null : nextCountry);
        l.setGeohash(GeohashUtil.encode(nextLat, nextLng, 9));
        boolean availableUnlimited = req.isAvailableUnlimited();
        l.setAvailableUnlimited(availableUnlimited);
        l.setAvailableFrom(availableUnlimited ? null : req.getAvailableFrom());
        l.setAvailableTo(null);
        if (req.getPickupLocationId() != null) {
            com.nearshare.api.model.ExchangeLocation pickup = exchangeLocationRepository.findById(req.getPickupLocationId())
                    .orElseThrow(() -> new RuntimeException("pickup_location_not_found"));
            l.setPickupLocation(pickup);
            l.setPickupLocationCustom(null);
            l.setPickupLocationStreet(null);
            l.setPickupLocationHouseNumber(null);
            l.setPickupLocationCity(null);
            l.setPickupLocationZip(null);
        } else {
            l.setPickupLocation(null);
            String pickupStreet = safePickupStreet(req.getPickupLocationStreet());
            String pickupHouse = safePickupHouseNumber(req.getPickupLocationHouseNumber());
            String pickupCity = safePickupCity(req.getPickupLocationCity());
            String pickupZip = safePickupZip(req.getPickupLocationZip());
            l.setPickupLocationCustom(formatPickupCustom(pickupStreet, pickupHouse, pickupCity, pickupZip, safePickupCustom(req.getPickupLocationCustom())));
            l.setPickupLocationStreet(pickupStreet);
            l.setPickupLocationHouseNumber(pickupHouse);
            l.setPickupLocationCity(pickupCity);
            l.setPickupLocationZip(pickupZip);
        }
        listingRepository.save(l);
        return toDTO(l, current);
    }

    public List<ListingDTO> findNearby(double borrowerLat, double borrowerLng, double radiusKm, int size) {
        var rows = listingRepository.findNearby(borrowerLat, borrowerLng, Math.max(0.1, radiusKm), PageRequest.of(0, Math.max(1, Math.min(200, size))));
        if (rows == null || rows.isEmpty()) return List.of();
        var ids = rows.stream().map(ListingRepository.ListingDistanceRow::getId).toList();
        var listings = listingRepository.findAllById(ids);
        var byId = listings.stream().collect(java.util.stream.Collectors.toMap(Listing::getId, x -> x));
        List<ListingDTO> out = new java.util.ArrayList<>();
        for (var row : rows) {
            var l = byId.get(row.getId());
            if (l == null) continue;
            out.add(toDTO(l, null, borrowerLat, borrowerLng));
        }
        return out;
    }

    private String safeText(String v) {
        String s = v == null ? "" : v.trim();
        return s;
    }

    @Transactional
    public void delete(UUID id, User current) {
        Listing l = listingRepository.findById(id).orElseThrow(() -> new RuntimeException("listing_not_found"));
        boolean isAdmin = current != null && current.getRole() == UserRole.ADMIN;
        boolean isOwner = current != null && l.getOwner() != null && l.getOwner().getId() != null && l.getOwner().getId().equals(current.getId());

        if (!isAdmin && !isOwner) {
            throw new RuntimeException("forbidden");
        }

        if (!isAdmin) {
            AvailabilityStatus st = l.getStatus();
            if (st == AvailabilityStatus.PENDING || st == AvailabilityStatus.APPROVED || st == AvailabilityStatus.READY_FOR_PICKUP || st == AvailabilityStatus.WAITING_FOR_RETURN || st == AvailabilityStatus.PARTNER_ACTIVE || st == AvailabilityStatus.BORROWED) {
                throw new RuntimeException("cannot_delete_active_listing");
            }
        }

        var reviews = reviewRepository.findByListing(l);
        if (!reviews.isEmpty()) {
            reviewRepository.deleteAll(reviews);
        }

        var reports = reportRepository.findByListing(l);
        if (!reports.isEmpty()) {
            reportRepository.deleteAll(reports);
        }

        var dismissals = dismissRepository.findByListing(l);
        if (!dismissals.isEmpty()) {
            dismissRepository.deleteAll(dismissals);
        }

        var txs = transactionRepository.findByListingId(l.getId());
        if (!txs.isEmpty()) {
            transactionRepository.deleteAll(txs);
        }

        returnSessionRepository.deleteByListingId(l.getId());

        listingRepository.delete(l);
    }

    @Transactional
    public ListingDTO borrow(UUID id, User borrower, com.nearshare.api.dto.BorrowRequest request) {
        Listing l = listingRepository.findById(id).orElseThrow(() -> new RuntimeException("listing_not_found"));

        if (l.getPartner() != null) {
            AvailabilityStatus st = l.getStatus();
            if (l.getBorrower() != null) {
                throw new RuntimeException("not_available");
            }
            if (st != AvailabilityStatus.PARTNER_ACTIVE && st != AvailabilityStatus.AVAILABLE) {
                throw new RuntimeException("not_available");
            }
            l.setBorrower(borrower);
            l.setStatus(AvailabilityStatus.PARTNER_BORROW_REQUESTED);
            l.setPartnerBorrowRequestedAt(java.time.LocalDateTime.now());
            l.setPartnerBorrowRequestedBy(borrower != null ? borrower.getId() : null);
            l.setPartnerBorrowReviewedAt(null);
            l.setPartnerBorrowReviewedBy(null);
            l.setPartnerBorrowRejectionReason(null);
            listingRepository.save(l);
            return toDTO(l, borrower);
        }

        if (l.getType() == ListingType.GIVE) {
            l.setBorrower(borrower);
            if (l.isAutoApprove()) {
                l.setStatus(AvailabilityStatus.GIFTED);
            } else {
                l.setStatus(AvailabilityStatus.PENDING);
            }
            listingRepository.save(l);
            return toDTO(l, borrower);
        }

        if (request.getPaymentToken() != null && !request.getPaymentToken().isBlank()) {
            var existing = transactionRepository.findByPaymentToken(request.getPaymentToken());
            if (existing.isPresent()) {
                var tx = existing.get();
                if (tx.getPayer() != null && tx.getPayer().getId() != null && !tx.getPayer().getId().equals(borrower.getId())) {
                    throw new RuntimeException("payment_token_already_used");
                }

                if (l.getBorrower() == null && l.getStatus() == AvailabilityStatus.AVAILABLE) {
                    throw new RuntimeException("payment_token_already_used");
                }
                return toDTO(l, borrower);
            }
        }
        
        // Calculate amount
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal hourlyRate = l.getHourlyRate() != null ? l.getHourlyRate() : BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal serviceFee = BigDecimal.ZERO;
        BigDecimal depositAmount = BigDecimal.ZERO;
        boolean isTimeBased = l.getType() != ListingType.GIVE && l.getType() != ListingType.SELL;
        int duration = isTimeBased ? (request.getDurationHours() > 0 ? request.getDurationHours() : 1) : 1;
        totalCost = hourlyRate.multiply(BigDecimal.valueOf(duration));

        String borrowerPath = request.getBorrowerPath() != null ? request.getBorrowerPath().toUpperCase() : "VERIFIED";
        boolean subscriptionEnabled = subscriptionService == null || subscriptionService.isSubscriptionEnabled();
        if (l.getType() == ListingType.LEND && !subscriptionEnabled) {
            BigDecimal fixed = BigDecimal.valueOf(runtimeSettingsService != null ? runtimeSettingsService.getDouble("settings.service.fee", 2.99) : 2.99);
            if (fixed.compareTo(BigDecimal.ZERO) > 0) {
                serviceFee = fixed.setScale(2, java.math.RoundingMode.HALF_UP);
            }
        } else if ("FEE".equals(borrowerPath)) {
            serviceFee = totalCost.multiply(new BigDecimal("0.08")).setScale(2, java.math.RoundingMode.HALF_UP);
        } else if ("DEPOSIT".equals(borrowerPath)) {
            depositAmount = new BigDecimal("50.00");
        }

        amount = totalCost.add(serviceFee).add(depositAmount);

        // Process payment if amount > 0 and payment method is not CASH
        if (amount.compareTo(BigDecimal.ZERO) > 0 && request.getPaymentMethod() != null && !"CASH".equalsIgnoreCase(request.getPaymentMethod())) {
             boolean success = paymentManager.processPayment(
                 request.getPaymentMethod(), 
                 amount, 
                 "USD", 
                 request.getPaymentToken()
             );
             
             if (!success) {
                 throw new RuntimeException("payment_failed");
             }
             
             // Save transaction
             String normalizedMethod = request.getPaymentMethod();
             if (normalizedMethod != null && "STRIPE".equalsIgnoreCase(normalizedMethod)) {
                 normalizedMethod = "CARD";
             }
             com.nearshare.api.model.Transaction t = com.nearshare.api.model.Transaction.builder()
                 .id(UUID.randomUUID())
                 .listing(l)
                 .payer(borrower)
                 .payee(l.getOwner())
                 .amount(amount)
                 .rentalAmount(totalCost)
                 .serviceFeeAmount(serviceFee)
                 .depositAmount(depositAmount)
                 .currency("USD")
                 .paymentMethod(normalizedMethod)
                 .paymentToken(request.getPaymentToken())
                 .borrowerPath(request.getBorrowerPath())
                 .timestamp(java.time.LocalDateTime.now())
                 .status("ESCROWED")
                 .build();
             transactionRepository.save(t);
        } else if (amount.compareTo(BigDecimal.ZERO) > 0 && "CASH".equalsIgnoreCase(request.getPaymentMethod())) {
             // Record CASH transaction
             com.nearshare.api.model.Transaction t = com.nearshare.api.model.Transaction.builder()
                 .id(UUID.randomUUID())
                 .listing(l)
                 .payer(borrower)
                 .payee(l.getOwner())
                 .amount(amount)
                 .rentalAmount(totalCost)
                 .serviceFeeAmount(serviceFee)
                 .depositAmount(depositAmount)
                 .currency("USD")
                 .paymentMethod("CASH")
                 .borrowerPath(request.getBorrowerPath())
                 .timestamp(java.time.LocalDateTime.now())
                 .status("PENDING")
                 .build();
             transactionRepository.save(t);
        }

        l.setBorrower(borrower);
        if (l.isAutoApprove()) {
            if (l.getType() == ListingType.GIVE) {
                l.setStatus(AvailabilityStatus.GIFTED);
                l.setItemReference(null);
            } else if (l.getType() == ListingType.SELL) {
                l.setStatus(AvailabilityStatus.SOLD);
                l.setItemReference(null);
            }
            else {
                l.setStatus(AvailabilityStatus.APPROVED);
                l.setItemReference(generateUniqueItemReference());
            }
        } else {
            l.setStatus(AvailabilityStatus.PENDING);
        }
        listingRepository.save(l);
        return toDTO(l, borrower);
    }

    @Transactional
    public ListingDTO approve(UUID id, User owner) {
        Listing l = listingRepository.findById(id).orElseThrow(() -> new RuntimeException("listing_not_found"));
        if (l.getPartner() != null) {
            throw new RuntimeException("forbidden");
        }
        boolean isAdmin = owner != null && owner.getRole() == UserRole.ADMIN;
        boolean isOwner = owner != null && l.getOwner() != null && l.getOwner().getId() != null && l.getOwner().getId().equals(owner.getId());
        if (!isAdmin && !isOwner) {
            throw new RuntimeException("forbidden");
        }
        if (l.getBorrower() == null) {
            throw new RuntimeException("borrower_not_found");
        }
        if (l.getType() != ListingType.GIVE && l.getType() != ListingType.SELL) {
            if (l.getStatus() != AvailabilityStatus.PENDING) {
                throw new RuntimeException("invalid_status");
            }
        }
        if (l.getType() == ListingType.GIVE) {
            l.setStatus(AvailabilityStatus.GIFTED);
            l.setItemReference(null);
            if (l.getBorrower() != null) {
                trustScoreService.updateTrustScore(l.getBorrower(), l);
                trustScoreService.updateTrustScore(owner, l);
            }
        } else if (l.getType() == ListingType.SELL) {
            l.setStatus(AvailabilityStatus.SOLD);
            l.setItemReference(null);
            if (l.getBorrower() != null) {
                trustScoreService.updateTrustScore(l.getBorrower(), l);
                trustScoreService.updateTrustScore(owner, l);
            }
        } else {
            l.setStatus(AvailabilityStatus.APPROVED);
            if (l.getItemReference() == null || l.getItemReference().isBlank()) {
                l.setItemReference(generateUniqueItemReference());
            }
        }
        listingRepository.save(l);
        return toDTO(l, owner);
    }

    @Transactional
    public ListingDTO deny(UUID id, User owner) {
        Listing l = listingRepository.findById(id).orElseThrow(() -> new RuntimeException("listing_not_found"));
        if (l.getPartner() != null) {
            throw new RuntimeException("forbidden");
        }
        boolean isAdmin = owner != null && owner.getRole() == UserRole.ADMIN;
        boolean isOwner = owner != null && l.getOwner() != null && l.getOwner().getId() != null && l.getOwner().getId().equals(owner.getId());
        if (!isAdmin && !isOwner) {
            throw new RuntimeException("forbidden");
        }
        l.setStatus(AvailabilityStatus.AVAILABLE);
        l.setBorrower(null);
        l.setItemReference(null);
        listingRepository.save(l);
        return toDTO(l, owner);
    }

    @Transactional
    public ListingDTO returnItem(UUID id, User owner) {
        Listing l = listingRepository.findById(id).orElseThrow(() -> new RuntimeException("listing_not_found"));
        if (l.getPartner() != null) {
            throw new RuntimeException("forbidden");
        }
        boolean isAdmin = owner != null && owner.getRole() == UserRole.ADMIN;
        boolean isOwner = owner != null && l.getOwner() != null && l.getOwner().getId() != null && l.getOwner().getId().equals(owner.getId());
        boolean isBorrower = owner != null && l.getBorrower() != null && l.getBorrower().getId() != null && l.getBorrower().getId().equals(owner.getId());
        if (!isAdmin && !isOwner && !isBorrower) {
            throw new RuntimeException("forbidden");
        }
        
        if (l.getBorrower() != null) {
            trustScoreService.updateTrustScore(l.getBorrower(), l);
            trustScoreService.updateTrustScore(owner, l);
        }
        
        l.setStatus(AvailabilityStatus.AVAILABLE);
        l.setBorrower(null);
        l.setItemReference(null);
        listingRepository.save(l);
        return toDTO(l, owner);
    }

    @Transactional
    public ListingDTO markReadyForPickup(UUID id, User current) {
        Listing l = listingRepository.findById(id).orElseThrow(() -> new RuntimeException("listing_not_found"));
        if (l.getPartner() != null) {
            throw new RuntimeException("forbidden");
        }
        if (l.getBorrower() == null) {
            throw new RuntimeException("borrower_not_found");
        }
        boolean isAdmin = current != null && current.getRole() == UserRole.ADMIN;
        boolean isOwner = current != null && l.getOwner() != null && l.getOwner().getId() != null && l.getOwner().getId().equals(current.getId());
        if (!isAdmin && !isOwner) {
            throw new RuntimeException("forbidden");
        }
        if (l.getStatus() != AvailabilityStatus.APPROVED) {
            throw new RuntimeException("invalid_status");
        }
        l.setStatus(AvailabilityStatus.READY_FOR_PICKUP);
        listingRepository.save(l);

        String pickupText = pickupLocationText(l);
        String msg = "Ready for pickup" + (pickupText.isEmpty() ? "." : (" at " + pickupText + "."));
        try {
            messageService.send(l.getOwner(), l.getBorrower(), msg, null);
        } catch (Exception ignored) {
        }
        try {
            emailService.sendPickupReadyEmail(l.getBorrower().getEmail(), l.getBorrower().getName(), l.getTitle(), pickupText);
        } catch (Exception ignored) {
        }

        return toDTO(l, current);
    }

    @Transactional
    public ListingDTO markPickedUp(UUID id, User current) {
        Listing l = listingRepository.findById(id).orElseThrow(() -> new RuntimeException("listing_not_found"));
        if (l.getPartner() != null) {
            throw new RuntimeException("forbidden");
        }
        boolean isAdmin = current != null && current.getRole() == UserRole.ADMIN;
        boolean isOwner = current != null && l.getOwner() != null && l.getOwner().getId() != null && l.getOwner().getId().equals(current.getId());
        boolean isBorrower = current != null && l.getBorrower() != null && l.getBorrower().getId() != null && l.getBorrower().getId().equals(current.getId());
        if (!isAdmin && !isOwner && !isBorrower) {
            throw new RuntimeException("forbidden");
        }
        if (l.getStatus() != AvailabilityStatus.READY_FOR_PICKUP) {
            throw new RuntimeException("invalid_status");
        }
        l.setStatus(AvailabilityStatus.WAITING_FOR_RETURN);
        if (l.getItemReference() == null || l.getItemReference().isBlank()) {
            l.setItemReference(generateUniqueItemReference());
        }
        listingRepository.save(l);
        return toDTO(l, current);
    }

    private String pickupLocationText(Listing l) {
        if (l == null) return "";
        if (l.getPickupLocation() != null) {
            String addr = l.getPickupLocation().getAddress();
            return addr == null ? "" : addr.trim();
        }
        String custom = l.getPickupLocationCustom();
        if (custom != null && !custom.trim().isEmpty()) return custom.trim();
        String street = l.getPickupLocationStreet();
        String house = l.getPickupLocationHouseNumber();
        String city = l.getPickupLocationCity();
        String zip = l.getPickupLocationZip();
        String combined = String.join(" ", safePickupStreet(street), safePickupHouseNumber(house)).trim();
        String combined2 = String.join(" ", safePickupCity(city), safePickupZip(zip)).trim();
        String out = (combined + (combined2.isEmpty() ? "" : (", " + combined2))).trim();
        return out;
    }

    @Transactional
    public ListingDTO block(UUID id) {
        Listing l = listingRepository.findById(id).orElseThrow(() -> new RuntimeException("listing_not_found"));
        if (l.getStatus() == AvailabilityStatus.BLOCKED) {
            l.setStatus(AvailabilityStatus.AVAILABLE);
        } else {
            l.setStatus(AvailabilityStatus.BLOCKED);
        }
        listingRepository.save(l);
        return toDTO(l, null);
    }

    @Transactional(readOnly = true)
    public List<ListingDTO> recommended(User current, int size) {
        if (current == null) return List.of();
        var dismissed = dismissRepository.findByUser(current).stream().map(d -> d.getListing().getId()).toList();
        List<Listing> all = listingRepository.findAll();
        List<Listing> candidates = all.stream()
                .filter(l -> l.getStatus() == AvailabilityStatus.AVAILABLE)
                .filter(l -> isAvailableForDiscovery(null, l))
                .filter(l -> l.getOwner() == null || !l.getOwner().getId().equals(current.getId()))
                .filter(l -> !dismissed.contains(l.getId()))
                .toList();
        record Scored(Listing l, double score) {}
        List<Scored> scored = candidates.stream().map(l -> {
            double score = 0.0;
            // proximity boost
            double dist = 0.0;
            if (current.getLocation() != null && l.getLocation() != null && current.getLocation().getLat() != null && current.getLocation().getLng() != null && l.getLocation().getLat() != null && l.getLocation().getLng() != null) {
                dist = DistanceUtil.haversineMiles(current.getLocation().getLat(), current.getLocation().getLng(), l.getLocation().getLat(), l.getLocation().getLng());
                score += Math.max(0, 10 - dist) / 10.0; // closer is better
            }
            // owner trust
            if (l.getOwner() != null) score += (l.getOwner().getTrustScore() / 100.0);
            // instant book
            if (l.isAutoApprove()) score += 0.3;
            // recency heuristic could be added if we track createdAt
            return new Scored(l, score);
        }).sorted((a,b) -> Double.compare(b.score, a.score)).toList();
        return scored.stream().limit(Math.max(1, size)).map(s -> toDTO(s.l, current)).toList();
    }

    public void report(UUID id, User reporter, String reason, String details) {
        if (reportRepository.existsByReporterIdAndListingIdAndReason(reporter.getId(), id, reason)) {
            throw new IllegalArgumentException("already_reported_for_reason");
        }
        Listing l = listingRepository.findById(id).orElseThrow(() -> new RuntimeException("listing_not_found"));
        Report r = Report.builder()
            .id(UUID.randomUUID())
            .listing(l)
            .reporter(reporter)
            .reason(reason)
            .details(details)
            .timestamp(java.time.LocalDateTime.now())
            .build();
        reportRepository.save(r);
    }

    public void dismiss(User current, UUID listingId) {
        Listing l = listingRepository.findById(listingId).orElseThrow(() -> new RuntimeException("listing_not_found"));
        if (dismissRepository.existsByUserAndListing(current, l)) return;
        var rec = com.nearshare.api.model.RecommendationDismiss.builder().id(UUID.randomUUID()).user(current).listing(l).createdAt(java.time.LocalDateTime.now()).build();
        dismissRepository.save(rec);
    }

    private ListingDTO toDTO(Listing l, User current) {
        return toDTO(l, current, null, null);
    }

    private ListingDTO toDTO(Listing l, User current, Double viewerLat, Double viewerLng) {
        double dist = 0;
        if (viewerLat != null && viewerLng != null && l.getLocation() != null && l.getLocation().getLat() != null && l.getLocation().getLng() != null) {
            dist = DistanceUtil.haversineMiles(viewerLat, viewerLng, l.getLocation().getLat(), l.getLocation().getLng());
        } else if (current != null && current.getLocation() != null && l.getLocation() != null && current.getLocation().getLat() != null && current.getLocation().getLng() != null && l.getLocation().getLat() != null && l.getLocation().getLng() != null) {
            dist = DistanceUtil.haversineMiles(current.getLocation().getLat(), current.getLocation().getLng(), l.getLocation().getLat(), l.getLocation().getLng());
        }

        boolean canSeeExactPickup = false;
        if (current != null && current.getId() != null) {
            if (l.getOwner() != null && current.getId().equals(l.getOwner().getId())) {
                canSeeExactPickup = true;
            } else if (l.getBorrower() != null && current.getId().equals(l.getBorrower().getId())) {
                AvailabilityStatus st = l.getStatus();
                canSeeExactPickup = st == AvailabilityStatus.APPROVED || st == AvailabilityStatus.PARTNER_ACTIVE || st == AvailabilityStatus.BORROWED || st == AvailabilityStatus.GIFTED || st == AvailabilityStatus.SOLD;
            }
        }
        String publicPickup = formatPickupCustom(null, null, l.getPickupLocationCity(), l.getPickupLocationZip(), null);

        return ListingDTO.builder()
            .id(l.getId())
            .itemReference(l.getItemReference())
            .ownerId(l.getOwner() != null ? l.getOwner().getId() : null)
            .partnerId(l.getPartner() != null ? l.getPartner().getId() : null)
            .partnerName(l.getPartner() != null ? l.getPartner().getName() : null)
            .partnerCity(l.getPartner() != null ? l.getPartner().getCity() : null)
            .partnerCreatedAt(l.getPartner() != null ? l.getPartner().getCreatedAt() : null)
            .borrowerId(l.getBorrower() != null ? l.getBorrower().getId() : null)
            .title(l.getTitle())
            .description(l.getDescription())
            .type(l.getType())
            .category(l.getCategory())
            .imageUrl(l.getImageUrl())
            .distanceMiles(dist)
            .status(l.getStatus())
            .hourlyRate(l.getHourlyRate())
            .location(LocationDTO.builder()
                .x(l.getLocation() != null ? l.getLocation().getLat() : null)
                .y(l.getLocation() != null ? l.getLocation().getLng() : null)
                .build())
            .owner(l.getOwner() != null ? UserSummaryDTO.builder()
                .id(l.getOwner().getId())
                .name(l.getOwner().getName())
                .trustScore(l.getOwner().getTrustScore())
                .avatarUrl(l.getOwner().getAvatarUrl())
                .address(l.getOwner().getAddress())
                .build() : null)
            .borrower(l.getBorrower() != null ? UserSummaryDTO.builder()
                .id(l.getBorrower().getId())
                .name(l.getBorrower().getName())
                .trustScore(l.getBorrower().getTrustScore())
                .avatarUrl(l.getBorrower().getAvatarUrl())
                .build() : null)
            .gallery(l.getGallery() != null ? new java.util.ArrayList<>(l.getGallery()) : null)
            .autoApprove(l.isAutoApprove())
            .insuranceRequired(l.isInsuranceRequired())
            .pickupLocation(l.getPickupLocation() != null ? ExchangeLocationDTO.builder()
                .id(l.getPickupLocation().getId())
                .referenceId(l.getPickupLocation().getReferenceId())
                .name(l.getPickupLocation().getName())
                .address(l.getPickupLocation().getAddress())
                .location(LocationDTO.builder()
                    .x(l.getPickupLocation().getLocation() != null ? l.getPickupLocation().getLocation().getLat() : null)
                    .y(l.getPickupLocation().getLocation() != null ? l.getPickupLocation().getLocation().getLng() : null)
                    .build())
                .build() : null)
            .pickupLocationCustom(canSeeExactPickup ? l.getPickupLocationCustom() : publicPickup)
            .pickupLocationStreet(canSeeExactPickup ? l.getPickupLocationStreet() : null)
            .pickupLocationHouseNumber(canSeeExactPickup ? l.getPickupLocationHouseNumber() : null)
            .pickupLocationCity(l.getPickupLocationCity())
            .pickupLocationZip(l.getPickupLocationZip())
            .availableUnlimited(l.isAvailableUnlimited())
            .availableFrom(l.getAvailableFrom())
            .availableTo(l.getAvailableTo())
            .build();
    }

    private double distanceForSort(Double viewerLat, Double viewerLng, Listing l) {
        if (viewerLat == null || viewerLng == null) return Double.MAX_VALUE;
        if (l == null || l.getLocation() == null || l.getLocation().getLat() == null || l.getLocation().getLng() == null) return Double.MAX_VALUE;
        return DistanceUtil.haversineMiles(viewerLat, viewerLng, l.getLocation().getLat(), l.getLocation().getLng());
    }

    private boolean isAvailableForDiscovery(User current, Listing l) {
        if (l == null) return false;
        if (canBypassAvailability(current, l)) return true;
        if (l.isAvailableUnlimited()) return true;
        java.time.LocalDateTime now = nowUtc();
        if (l.getAvailableFrom() != null && l.getAvailableFrom().isAfter(now)) return false;
        if (l.getAvailableTo() != null && l.getAvailableTo().isBefore(now)) return false;
        return true;
    }

    private boolean canBypassAvailability(User current, Listing l) {
        if (current == null || current.getId() == null || l == null) return false;
        if (current.getRole() == UserRole.ADMIN) return true;
        return l.getOwner() != null && l.getOwner().getId() != null && l.getOwner().getId().equals(current.getId());
    }

    private java.time.LocalDateTime nowUtc() {
        return java.time.LocalDateTime.now(java.time.Clock.systemUTC());
    }

    private String generateUniqueItemReference() {
        for (int attempt = 0; attempt < 25; attempt++) {
            int v = itemRefRandom.nextInt(100_000_000);
            String code = String.format("%08d", v);
            if (!listingRepository.existsByItemReference(code)) {
                return code;
            }
        }
        throw new RuntimeException("failed_to_generate_item_reference");
    }

    private String safePickupCustom(String raw) {
        if (raw == null) return null;
        String s = raw.trim().replaceAll("\\s+", " ");
        if (s.isBlank()) return null;
        if (s.length() > 280) s = s.substring(0, 280);
        return s;
    }

    private String safePickupCity(String raw) {
        if (raw == null) return null;
        String s = raw.trim().replaceAll("\\s+", " ");
        if (s.isBlank()) return null;
        if (s.length() > 80) s = s.substring(0, 80);
        return s;
    }

    private String safePickupZip(String raw) {
        if (raw == null) return null;
        String s = raw.trim().replaceAll("\\s+", " ");
        if (s.isBlank()) return null;
        if (s.length() > 20) s = s.substring(0, 20);
        return s;
    }

    private String safePickupStreet(String raw) {
        if (raw == null) return null;
        String s = raw.trim().replaceAll("\\s+", " ");
        if (s.isBlank()) return null;
        if (s.length() > 120) s = s.substring(0, 120);
        return s;
    }

    private String safePickupHouseNumber(String raw) {
        if (raw == null) return null;
        String s = raw.trim().replaceAll("\\s+", " ");
        if (s.isBlank()) return null;
        if (s.length() > 20) s = s.substring(0, 20);
        return s;
    }

    private String formatPickupCustom(String street, String houseNumber, String city, String zip, String legacyCustom) {
        String streetPart = null;
        if (street != null && houseNumber != null) streetPart = street + " " + houseNumber;
        else if (street != null) streetPart = street;
        else if (houseNumber != null) streetPart = houseNumber;

        String cityPart = null;
        if (city != null && zip != null) cityPart = city + " " + zip;
        else if (city != null) cityPart = city;
        else if (zip != null) cityPart = zip;

        if (streetPart != null && cityPart != null) return streetPart + ", " + cityPart;
        if (streetPart != null) return streetPart;
        if (cityPart != null) return cityPart;
        return legacyCustom;
    }

    @org.springframework.transaction.annotation.Transactional
    public void completeTransaction(String paymentToken, String listingIdStr, String borrowerIdStr, String borrowerPath, BigDecimal amount, int durationHours) {
        if (transactionRepository.findByPaymentToken(paymentToken).isPresent()) {
            return;
        }

        UUID listingId = UUID.fromString(listingIdStr);
        UUID borrowerId = UUID.fromString(borrowerIdStr);

        Listing l = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("listing_not_found"));
        
        User borrower = userRepository.findById(borrowerId)
                .orElseThrow(() -> new RuntimeException("borrower_not_found"));

        BigDecimal hourlyRate = l.getHourlyRate() != null ? l.getHourlyRate() : BigDecimal.ZERO;
        boolean isTimeBased = l.getType() != ListingType.GIVE && l.getType() != ListingType.SELL;
        int duration = isTimeBased ? (durationHours > 0 ? durationHours : 1) : 1;
        BigDecimal totalCost = hourlyRate.multiply(BigDecimal.valueOf(duration));
        BigDecimal serviceFee = BigDecimal.ZERO;
        BigDecimal depositAmount = BigDecimal.ZERO;
        String bp = borrowerPath != null ? borrowerPath.toUpperCase() : "VERIFIED";
        if ("FEE".equals(bp)) {
            serviceFee = totalCost.multiply(new BigDecimal("0.08")).setScale(2, java.math.RoundingMode.HALF_UP);
        } else if ("DEPOSIT".equals(bp)) {
            depositAmount = new BigDecimal("50.00");
        }

        com.nearshare.api.model.Transaction t = com.nearshare.api.model.Transaction.builder()
                .id(UUID.randomUUID())
                .listing(l)
                .payer(borrower)
                .payee(l.getOwner())
                .amount(amount)
                .rentalAmount(totalCost)
                .serviceFeeAmount(serviceFee)
                .depositAmount(depositAmount)
                .currency("USD")
                .paymentMethod("CARD")
                .paymentToken(paymentToken)
                .borrowerPath(borrowerPath)
                .timestamp(java.time.LocalDateTime.now())
                .status("ESCROWED")
                .build();
        transactionRepository.save(t);

        l.setBorrower(borrower);
        if (l.isAutoApprove()) {
             if (l.getType() == ListingType.GIVE) l.setStatus(AvailabilityStatus.GIFTED);
             else if (l.getType() == ListingType.SELL) l.setStatus(AvailabilityStatus.SOLD);
             else l.setStatus(AvailabilityStatus.BORROWED);
        } else {
             l.setStatus(AvailabilityStatus.PENDING);
        }
        listingRepository.save(l);
    }
}
