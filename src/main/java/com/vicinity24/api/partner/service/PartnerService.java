package com.vicinity24.api.partner.service;

import com.vicinity24.api.dto.ListingDTO;
import com.vicinity24.api.dto.LocationDTO;
import com.vicinity24.api.dto.ExchangeLocationDTO;
import com.vicinity24.api.dto.UserSummaryDTO;
import com.vicinity24.api.model.Listing;
import com.vicinity24.api.model.User;
import com.vicinity24.api.model.embeddable.Location;
import com.vicinity24.api.model.enums.AvailabilityStatus;
import com.vicinity24.api.partner.dto.PartnerBorrowRequestDTO;
import com.vicinity24.api.partner.dto.PartnerCreateListingRequest;
import com.vicinity24.api.partner.dto.PartnerDTO;
import com.vicinity24.api.partner.dto.PartnerRegistrationRequest;
import com.vicinity24.api.partner.dto.PartnerReturnRequestDTO;
import com.vicinity24.api.partner.dto.PartnerSettingsDTO;
import com.vicinity24.api.partner.model.Partner;
import com.vicinity24.api.partner.model.PartnerAdmin;
import com.vicinity24.api.partner.model.PartnerAdminRole;
import com.vicinity24.api.partner.model.PartnerSettings;
import com.vicinity24.api.partner.model.PartnerStatus;
import com.vicinity24.api.partner.repository.PartnerAdminRepository;
import com.vicinity24.api.partner.repository.PartnerRepository;
import com.vicinity24.api.partner.repository.PartnerSettingsRepository;
import com.vicinity24.api.repository.ListingRepository;
import com.vicinity24.api.repository.ExchangeLocationRepository;
import com.vicinity24.api.repository.ReturnSessionRepository;
import com.vicinity24.api.service.ReturnService;
import com.vicinity24.api.util.DistanceUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PartnerService {
    private final PartnerRepository partnerRepository;
    private final PartnerAdminRepository partnerAdminRepository;
    private final PartnerSettingsRepository partnerSettingsRepository;
    private final ListingRepository listingRepository;
    private final ExchangeLocationRepository exchangeLocationRepository;
    private final SecureRandom itemRefRandom = new SecureRandom();
    private final ReturnSessionRepository returnSessionRepository;
    private final ReturnService returnService;

    public PartnerService(
            PartnerRepository partnerRepository,
            PartnerAdminRepository partnerAdminRepository,
            PartnerSettingsRepository partnerSettingsRepository,
            ListingRepository listingRepository,
            ExchangeLocationRepository exchangeLocationRepository,
            ReturnSessionRepository returnSessionRepository,
            ReturnService returnService) {
        this.partnerRepository = partnerRepository;
        this.partnerAdminRepository = partnerAdminRepository;
        this.partnerSettingsRepository = partnerSettingsRepository;
        this.listingRepository = listingRepository;
        this.exchangeLocationRepository = exchangeLocationRepository;
        this.returnSessionRepository = returnSessionRepository;
        this.returnService = returnService;
    }

    @Transactional(readOnly = true)
    public List<PartnerDTO> getMyPartners(User current) {
        var links = partnerAdminRepository.findAllByUserId(current.getId());
        return links.stream()
                .map(pa -> pa.getPartner())
                .distinct()
                .map(this::toPartnerDTO)
                .toList();
    }

    @Transactional
    public PartnerDTO registerPartner(User current, PartnerRegistrationRequest req) {
        Partner p = Partner.builder()
                .id(UUID.randomUUID())
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .address(req.getAddress())
                .city(req.getCity())
                .contactPerson(req.getContactPerson())
                .status(PartnerStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        partnerRepository.save(p);

        PartnerAdmin admin = PartnerAdmin.builder()
                .id(UUID.randomUUID())
                .partner(p)
                .user(current)
                .role(PartnerAdminRole.ADMIN)
                .createdAt(LocalDateTime.now())
                .build();
        partnerAdminRepository.save(admin);

        return toPartnerDTO(p);
    }

    @Transactional
    public ListingDTO createPartnerListing(User current, PartnerCreateListingRequest req) {
        if (req.getPartnerId() == null) throw new RuntimeException("partner_id_required");
        requirePartnerAdmin(current, req.getPartnerId());
        Partner partner = partnerRepository.findById(req.getPartnerId()).orElseThrow(() -> new RuntimeException("partner_not_found"));

        com.vicinity24.api.model.ExchangeLocation pickup = null;
        if (req.getPickupLocationId() != null) {
            pickup = exchangeLocationRepository.findById(req.getPickupLocationId())
                    .orElseThrow(() -> new RuntimeException("pickup_location_not_found"));
        }
        String pickupStreet = req.getPickupLocationId() == null ? safePickupStreet(req.getPickupLocationStreet()) : null;
        String pickupHouse = req.getPickupLocationId() == null ? safePickupHouseNumber(req.getPickupLocationHouseNumber()) : null;
        String pickupCity = req.getPickupLocationId() == null ? safePickupCity(req.getPickupLocationCity()) : null;
        String pickupZip = req.getPickupLocationId() == null ? safePickupZip(req.getPickupLocationZip()) : null;
        String pickupCustom = req.getPickupLocationId() == null ? formatPickupCustom(pickupStreet, pickupHouse, pickupCity, pickupZip, safePickupCustom(req.getPickupLocationCustom())) : null;

        BigDecimal hourlyRate = req.getHourlyRate();
        if (req.getType() == null) throw new RuntimeException("type_required");
        if (hourlyRate == null) hourlyRate = BigDecimal.ZERO;
        if (req.getType().name().equalsIgnoreCase("GIVE")) hourlyRate = BigDecimal.ZERO;
        boolean availableUnlimited = req.isAvailableUnlimited();
        LocalDateTime availableFrom = availableUnlimited ? null : req.getAvailableFrom();
        LocalDateTime availableTo = null;

        Listing l = Listing.builder()
                .id(UUID.randomUUID())
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .type(req.getType())
                .imageUrl(req.getImageUrl())
                .gallery(req.getGallery())
                .hourlyRate(hourlyRate)
                .autoApprove(req.isAutoApprove())
                .insuranceRequired(req.isInsuranceRequired())
                .status(AvailabilityStatus.PARTNER_INACTIVE)
                .location(Location.builder().lat(req.getX()).lng(req.getY()).build())
                .owner(null)
                .partner(partner)
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
                .partnerSubmittedAt(LocalDateTime.now())
                .partnerSubmittedBy(current.getId())
                .createdAt(LocalDateTime.now())
                .build();
        listingRepository.save(l);
        return toListingDTO(l, current);
    }

    @Transactional(readOnly = true)
    public List<ListingDTO> getPartnerListings(User current) {
        Set<UUID> partnerIds = partnerAdminRepository.findAllByUserId(current.getId()).stream()
                .map(pa -> pa.getPartner().getId())
                .collect(Collectors.toSet());
        if (partnerIds.isEmpty()) return List.of();

        return listingRepository.findAll().stream()
                .filter(l -> l.getPartner() != null && partnerIds.contains(l.getPartner().getId()))
                .sorted((a, b) -> {
                    var at = a.getCreatedAt();
                    var bt = b.getCreatedAt();
                    if (at == null && bt == null) return 0;
                    if (at == null) return 1;
                    if (bt == null) return -1;
                    return bt.compareTo(at);
                })
                .map(l -> toListingDTO(l, current))
                .toList();
    }

    @Transactional
    public ListingDTO updatePartnerListing(User current, UUID listingId, PartnerCreateListingRequest req) {
        Listing l = listingRepository.findById(listingId).orElseThrow(() -> new RuntimeException("listing_not_found"));
        if (l.getPartner() == null) throw new RuntimeException("not_partner_listing");
        requirePartnerAdmin(current, l.getPartner().getId());

        l.setTitle(req.getTitle());
        l.setDescription(req.getDescription());
        l.setCategory(req.getCategory());
        l.setType(req.getType());
        l.setImageUrl(req.getImageUrl());
        l.setGallery(req.getGallery());
        l.setInsuranceRequired(req.isInsuranceRequired());
        l.setAutoApprove(req.isAutoApprove());
        BigDecimal hourlyRate = req.getHourlyRate();
        if (req.getType() == null) throw new RuntimeException("type_required");
        if (hourlyRate == null) hourlyRate = BigDecimal.ZERO;
        if (req.getType().name().equalsIgnoreCase("GIVE")) hourlyRate = BigDecimal.ZERO;
        l.setHourlyRate(hourlyRate);
        l.setLocation(Location.builder().lat(req.getX()).lng(req.getY()).build());
        boolean availableUnlimited = req.isAvailableUnlimited();
        l.setAvailableUnlimited(availableUnlimited);
        l.setAvailableFrom(availableUnlimited ? null : req.getAvailableFrom());
        l.setAvailableTo(null);
        if (l.getStatus() == AvailabilityStatus.PARTNER_INACTIVE && l.getPartnerSubmittedAt() == null) {
            l.setPartnerSubmittedAt(LocalDateTime.now());
            l.setPartnerSubmittedBy(current.getId());
        }

        if (req.getPickupLocationId() != null) {
            com.vicinity24.api.model.ExchangeLocation pickup = exchangeLocationRepository.findById(req.getPickupLocationId())
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
        return toListingDTO(l, current);
    }

    @Transactional
    public void deletePartnerListing(User current, UUID listingId) {
        Listing l = listingRepository.findById(listingId).orElseThrow(() -> new RuntimeException("listing_not_found"));
        if (l.getPartner() == null) throw new RuntimeException("not_partner_listing");
        requirePartnerAdmin(current, l.getPartner().getId());

        AvailabilityStatus st = l.getStatus();
        if (st == AvailabilityStatus.PENDING || st == AvailabilityStatus.PARTNER_ACTIVE || st == AvailabilityStatus.BORROWED) {
            throw new RuntimeException("cannot_delete_active_listing");
        }
        listingRepository.delete(l);
    }

    @Transactional(readOnly = true)
    public List<PartnerBorrowRequestDTO> getPartnerRequests(User current) {
        Set<UUID> partnerIds = partnerAdminRepository.findAllByUserId(current.getId()).stream()
                .map(pa -> pa.getPartner().getId())
                .collect(Collectors.toSet());
        if (partnerIds.isEmpty()) return List.of();

        return listingRepository.findAll().stream()
                .filter(l -> l.getPartner() != null && partnerIds.contains(l.getPartner().getId()))
                .filter(l -> l.getStatus() == AvailabilityStatus.PARTNER_BORROW_REQUESTED)
                .map(l -> PartnerBorrowRequestDTO.builder()
                        .listingId(l.getId())
                        .listingTitle(l.getTitle())
                        .partnerId(l.getPartner().getId())
                        .partnerName(l.getPartner().getName())
                        .borrowerId(l.getBorrower() != null ? l.getBorrower().getId() : null)
                        .borrowerName(l.getBorrower() != null ? l.getBorrower().getName() : null)
                        .borrowerEmail(l.getBorrower() != null ? l.getBorrower().getEmail() : null)
                        .status(l.getStatus())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PartnerReturnRequestDTO> getPendingManualReturns(User current) {
        Set<UUID> partnerIds = partnerAdminRepository.findAllByUserId(current.getId()).stream()
                .map(pa -> pa.getPartner() != null ? pa.getPartner().getId() : null)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (partnerIds.isEmpty()) return List.of();

        return returnSessionRepository
                .findByStatusAndManualBorrowerConfirmedAtIsNotNullAndManualLenderConfirmedAtIsNullOrderByCreatedAtDesc(com.vicinity24.api.model.enums.ReturnStatus.PENDING)
                .stream()
                .filter(rs -> rs.getListing() != null && rs.getListing().getPartner() != null && partnerIds.contains(rs.getListing().getPartner().getId()))
                .map(rs -> PartnerReturnRequestDTO.builder()
                        .listingId(rs.getListing().getId())
                        .listingTitle(rs.getListing().getTitle())
                        .itemReference(rs.getListing().getItemReference())
                        .partnerId(rs.getListing().getPartner() != null ? rs.getListing().getPartner().getId() : null)
                        .partnerName(rs.getListing().getPartner() != null ? rs.getListing().getPartner().getName() : null)
                        .borrowerId(rs.getBorrower() != null ? rs.getBorrower().getId() : null)
                        .borrowerName(rs.getBorrower() != null ? rs.getBorrower().getName() : null)
                        .borrowerEmail(rs.getBorrower() != null ? rs.getBorrower().getEmail() : null)
                        .borrowerConfirmedAt(rs.getManualBorrowerConfirmedAt())
                        .build())
                .toList();
    }

    @Transactional
    public com.vicinity24.api.dto.ReturnDTOs.ReturnSessionResponse acceptManualReturn(User current, UUID listingId) {
        Listing listing = listingRepository.findById(listingId).orElseThrow(() -> new RuntimeException("listing_not_found"));
        if (listing.getPartner() == null || listing.getPartner().getId() == null) throw new RuntimeException("not_partner_listing");
        requirePartnerAdmin(current, listing.getPartner().getId());
        String itemRef = listing.getItemReference();
        if (itemRef == null || itemRef.isBlank()) {
            itemRef = generateUniqueItemReference();
            listing.setItemReference(itemRef);
            listingRepository.save(listing);
        }
        com.vicinity24.api.dto.ReturnDTOs.ManualFallbackRequest req = new com.vicinity24.api.dto.ReturnDTOs.ManualFallbackRequest();
        req.setItemNumber(itemRef);
        req.setConciergeWitnessId("PARTNER_ADMIN:" + current.getId());
        return returnService.manualFallback(listingId, current, req);
    }

    @Transactional
    public com.vicinity24.api.dto.ReturnDTOs.ReturnSessionResponse denyManualReturn(User current, UUID listingId, String reason) {
        Listing listing = listingRepository.findById(listingId).orElseThrow(() -> new RuntimeException("listing_not_found"));
        if (listing.getPartner() == null || listing.getPartner().getId() == null) throw new RuntimeException("not_partner_listing");
        requirePartnerAdmin(current, listing.getPartner().getId());
        return returnService.denyManualReturn(listingId, current, reason);
    }

    @Transactional
    public ListingDTO approvePartnerRequest(User current, UUID listingId) {
        Listing l = listingRepository.findById(listingId).orElseThrow(() -> new RuntimeException("listing_not_found"));
        if (l.getPartner() == null) throw new RuntimeException("not_partner_listing");
        requirePartnerAdmin(current, l.getPartner().getId());
        if (l.getStatus() != AvailabilityStatus.PARTNER_BORROW_REQUESTED) throw new RuntimeException("invalid_status");

        if (l.getType() != null && l.getType().name().equalsIgnoreCase("GIVE")) {
            l.setStatus(AvailabilityStatus.GIFTED);
            l.setItemReference(null);
        } else if (l.getType() != null && l.getType().name().equalsIgnoreCase("SELL")) {
            l.setStatus(AvailabilityStatus.SOLD);
            l.setItemReference(null);
        } else {
            l.setStatus(AvailabilityStatus.BORROWED);
            l.setItemReference(generateUniqueItemReference());
        }
        l.setPartnerBorrowReviewedAt(LocalDateTime.now());
        l.setPartnerBorrowReviewedBy(current.getId());
        l.setPartnerBorrowRejectionReason(null);
        listingRepository.save(l);
        return toListingDTO(l, current);
    }

    @Transactional
    public ListingDTO rejectPartnerRequest(User current, UUID listingId) {
        Listing l = listingRepository.findById(listingId).orElseThrow(() -> new RuntimeException("listing_not_found"));
        if (l.getPartner() == null) throw new RuntimeException("not_partner_listing");
        requirePartnerAdmin(current, l.getPartner().getId());
        if (l.getStatus() != AvailabilityStatus.PARTNER_BORROW_REQUESTED) throw new RuntimeException("invalid_status");

        l.setStatus(AvailabilityStatus.PARTNER_ACTIVE);
        l.setBorrower(null);
        l.setItemReference(null);
        l.setPartnerBorrowReviewedAt(LocalDateTime.now());
        l.setPartnerBorrowReviewedBy(current.getId());
        l.setPartnerBorrowRejectionReason("partner_reject_borrow_request");
        listingRepository.save(l);
        return toListingDTO(l, current);
    }

    @Transactional(readOnly = true)
    public PartnerSettingsDTO getSettings(User current, UUID partnerId) {
        UUID pid = resolvePartnerId(current, partnerId);
        requirePartnerAdmin(current, pid);
        Partner partner = partnerRepository.findById(pid).orElseThrow(() -> new RuntimeException("partner_not_found"));
        PartnerSettings ps = partnerSettingsRepository.findByPartnerId(pid).orElseGet(() -> {
            PartnerSettings created = PartnerSettings.builder()
                    .id(UUID.randomUUID())
                    .partner(partner)
                    .maxLendingDays(14)
                    .depositCents(0)
                    .autoApproval(false)
                    .updatedAt(LocalDateTime.now())
                    .build();
            return partnerSettingsRepository.save(created);
        });
        return PartnerSettingsDTO.builder()
                .partnerId(pid)
                .maxLendingDays(ps.getMaxLendingDays())
                .depositCents(ps.getDepositCents())
                .autoApproval(ps.getAutoApproval())
                .build();
    }

    @Transactional
    public PartnerSettingsDTO updateSettings(User current, UUID partnerId, PartnerSettingsDTO dto) {
        UUID pid = resolvePartnerId(current, partnerId);
        requirePartnerAdmin(current, pid);
        Partner partner = partnerRepository.findById(pid).orElseThrow(() -> new RuntimeException("partner_not_found"));
        PartnerSettings ps = partnerSettingsRepository.findByPartnerId(pid).orElseGet(() -> PartnerSettings.builder()
                .id(UUID.randomUUID())
                .partner(partner)
                .maxLendingDays(14)
                .depositCents(0)
                .autoApproval(false)
                .updatedAt(LocalDateTime.now())
                .build());
        ps.setMaxLendingDays(dto.getMaxLendingDays());
        ps.setDepositCents(dto.getDepositCents());
        ps.setAutoApproval(dto.getAutoApproval());
        ps.setUpdatedAt(LocalDateTime.now());
        partnerSettingsRepository.save(ps);
        return PartnerSettingsDTO.builder()
                .partnerId(pid)
                .maxLendingDays(ps.getMaxLendingDays())
                .depositCents(ps.getDepositCents())
                .autoApproval(ps.getAutoApproval())
                .build();
    }

    private void requirePartnerAdmin(User current, UUID partnerId) {
        if (!partnerAdminRepository.existsByUserAndPartnerAndRole(current.getId(), partnerId, PartnerAdminRole.ADMIN)) {
            throw new RuntimeException("forbidden");
        }
    }

    private UUID resolvePartnerId(User current, UUID partnerId) {
        if (partnerId != null) return partnerId;
        List<UUID> ids = partnerAdminRepository.findAllByUserId(current.getId()).stream().map(pa -> pa.getPartner().getId()).distinct().toList();
        if (ids.size() != 1) throw new RuntimeException("partner_id_required");
        return ids.get(0);
    }

    private PartnerDTO toPartnerDTO(Partner p) {
        return PartnerDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .email(p.getEmail())
                .phone(p.getPhone())
                .address(p.getAddress())
                .city(p.getCity())
                .contactPerson(p.getContactPerson())
                .status(p.getStatus())
                .build();
    }

    private ListingDTO toListingDTO(Listing l, User current) {
        double dist = 0;
        if (current != null && current.getLocation() != null && l.getLocation() != null && current.getLocation().getLat() != null && current.getLocation().getLng() != null && l.getLocation().getLat() != null && l.getLocation().getLng() != null) {
            dist = DistanceUtil.haversineMiles(current.getLocation().getLat(), current.getLocation().getLng(), l.getLocation().getLat(), l.getLocation().getLng());
        }

        boolean canSeeExactPickup = false;
        if (current != null && current.getId() != null) {
            if (l.getOwner() != null && current.getId().equals(l.getOwner().getId())) {
                canSeeExactPickup = true;
            } else if (l.getBorrower() != null && current.getId().equals(l.getBorrower().getId())) {
                AvailabilityStatus st = l.getStatus();
                canSeeExactPickup = st == AvailabilityStatus.PARTNER_ACTIVE || st == AvailabilityStatus.BORROWED || st == AvailabilityStatus.GIFTED || st == AvailabilityStatus.SOLD;
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
}
