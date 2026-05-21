package com.nearshare.api.partner.service;

import com.nearshare.api.dto.ListingDTO;
import com.nearshare.api.dto.LocationDTO;
import com.nearshare.api.dto.PickupLocationDTO;
import com.nearshare.api.dto.UserSummaryDTO;
import com.nearshare.api.model.Listing;
import com.nearshare.api.model.User;
import com.nearshare.api.model.embeddable.Location;
import com.nearshare.api.model.enums.AvailabilityStatus;
import com.nearshare.api.partner.dto.PartnerBorrowRequestDTO;
import com.nearshare.api.partner.dto.PartnerCreateListingRequest;
import com.nearshare.api.partner.dto.PartnerDTO;
import com.nearshare.api.partner.dto.PartnerRegistrationRequest;
import com.nearshare.api.partner.dto.PartnerSettingsDTO;
import com.nearshare.api.partner.model.Partner;
import com.nearshare.api.partner.model.PartnerAdmin;
import com.nearshare.api.partner.model.PartnerAdminRole;
import com.nearshare.api.partner.model.PartnerSettings;
import com.nearshare.api.partner.model.PartnerStatus;
import com.nearshare.api.partner.repository.PartnerAdminRepository;
import com.nearshare.api.partner.repository.PartnerRepository;
import com.nearshare.api.partner.repository.PartnerSettingsRepository;
import com.nearshare.api.repository.ListingRepository;
import com.nearshare.api.repository.PickupLocationRepository;
import com.nearshare.api.util.DistanceUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final PickupLocationRepository pickupLocationRepository;

    public PartnerService(
            PartnerRepository partnerRepository,
            PartnerAdminRepository partnerAdminRepository,
            PartnerSettingsRepository partnerSettingsRepository,
            ListingRepository listingRepository,
            PickupLocationRepository pickupLocationRepository) {
        this.partnerRepository = partnerRepository;
        this.partnerAdminRepository = partnerAdminRepository;
        this.partnerSettingsRepository = partnerSettingsRepository;
        this.listingRepository = listingRepository;
        this.pickupLocationRepository = pickupLocationRepository;
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

        com.nearshare.api.model.PickupLocation pickup = null;
        if (req.getPickupLocationId() != null) {
            pickup = pickupLocationRepository.findById(req.getPickupLocationId())
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
        LocalDateTime availableTo = availableUnlimited ? null : req.getAvailableTo();

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
                .status(AvailabilityStatus.PARTNER_PENDING_APPROVAL)
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
        l.setAvailableTo(availableUnlimited ? null : req.getAvailableTo());
        if (l.getStatus() == AvailabilityStatus.PARTNER_PENDING_APPROVAL && l.getPartnerSubmittedAt() == null) {
            l.setPartnerSubmittedAt(LocalDateTime.now());
            l.setPartnerSubmittedBy(current.getId());
        }

        if (req.getPickupLocationId() != null) {
            com.nearshare.api.model.PickupLocation pickup = pickupLocationRepository.findById(req.getPickupLocationId())
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
        if (st == AvailabilityStatus.PENDING || st == AvailabilityStatus.APPROVED || st == AvailabilityStatus.BORROWED) {
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
                .filter(l -> l.getStatus() == AvailabilityStatus.PENDING)
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

    @Transactional
    public ListingDTO approvePartnerRequest(User current, UUID listingId) {
        Listing l = listingRepository.findById(listingId).orElseThrow(() -> new RuntimeException("listing_not_found"));
        if (l.getPartner() == null) throw new RuntimeException("not_partner_listing");
        requirePartnerAdmin(current, l.getPartner().getId());
        if (l.getStatus() != AvailabilityStatus.PENDING) throw new RuntimeException("invalid_status");

        if (l.getType() != null && l.getType().name().equalsIgnoreCase("GIVE")) {
            l.setStatus(AvailabilityStatus.GIFTED);
        } else if (l.getType() != null && l.getType().name().equalsIgnoreCase("SELL")) {
            l.setStatus(AvailabilityStatus.SOLD);
        } else {
            l.setStatus(AvailabilityStatus.APPROVED);
        }
        listingRepository.save(l);
        return toListingDTO(l, current);
    }

    @Transactional
    public ListingDTO rejectPartnerRequest(User current, UUID listingId) {
        Listing l = listingRepository.findById(listingId).orElseThrow(() -> new RuntimeException("listing_not_found"));
        if (l.getPartner() == null) throw new RuntimeException("not_partner_listing");
        requirePartnerAdmin(current, l.getPartner().getId());
        if (l.getStatus() != AvailabilityStatus.PENDING) throw new RuntimeException("invalid_status");

        l.setStatus(AvailabilityStatus.AVAILABLE);
        l.setBorrower(null);
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
                canSeeExactPickup = st == AvailabilityStatus.APPROVED || st == AvailabilityStatus.BORROWED || st == AvailabilityStatus.GIFTED || st == AvailabilityStatus.SOLD;
            }
        }
        String publicPickup = formatPickupCustom(null, null, l.getPickupLocationCity(), l.getPickupLocationZip(), null);

        return ListingDTO.builder()
                .id(l.getId())
                .ownerId(l.getOwner() != null ? l.getOwner().getId() : null)
                .partnerId(l.getPartner() != null ? l.getPartner().getId() : null)
                .partnerName(l.getPartner() != null ? l.getPartner().getName() : null)
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
                .pickupLocation(l.getPickupLocation() != null ? PickupLocationDTO.builder()
                        .id(l.getPickupLocation().getId())
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
