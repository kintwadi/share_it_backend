package com.nearshare.api.service;

import com.nearshare.api.dto.CreateListingRequest;
import com.nearshare.api.dto.ListingDTO;
import com.nearshare.api.dto.LocationDTO;
import com.nearshare.api.dto.UserSummaryDTO;
import com.nearshare.api.model.Listing;
import com.nearshare.api.model.User;
import com.nearshare.api.model.Report;
import com.nearshare.api.model.embeddable.Location;
import com.nearshare.api.model.enums.AvailabilityStatus;
import com.nearshare.api.repository.ListingRepository;
import com.nearshare.api.repository.UserRepository;
import com.nearshare.api.repository.ReportRepository;
import com.nearshare.api.util.DistanceUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Service
public class ListingService {
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final com.nearshare.api.repository.RecommendationDismissRepository dismissRepository;
    private final com.nearshare.api.payment.PaymentManager paymentManager;
    private final com.nearshare.api.repository.TransactionRepository transactionRepository;
    private final ReportRepository reportRepository;

    public ListingService(ListingRepository listingRepository, UserRepository userRepository, com.nearshare.api.repository.RecommendationDismissRepository dismissRepository, com.nearshare.api.payment.PaymentManager paymentManager, com.nearshare.api.repository.TransactionRepository transactionRepository, ReportRepository reportRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.dismissRepository = dismissRepository;
        this.paymentManager = paymentManager;
        this.transactionRepository = transactionRepository;
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public Page<ListingDTO> findAll(User current, String search, String category, String type, Double minPrice, int page, int size) {
        List<Listing> all = listingRepository.findAll();
        List<Listing> filtered = all.stream()
            .filter(l -> l.getStatus() == null || (l.getStatus() != AvailabilityStatus.BLOCKED && l.getStatus() != AvailabilityStatus.HIDDEN))
            .filter(l -> search == null || (l.getTitle() != null && l.getTitle().toLowerCase().contains(search.toLowerCase())))
            .filter(l -> category == null || (l.getCategory() != null && l.getCategory().equalsIgnoreCase(category)))
            .filter(l -> type == null || (l.getType() != null && l.getType().name().equalsIgnoreCase(type)))
            .filter(l -> minPrice == null || (l.getHourlyRate() != null && l.getHourlyRate().compareTo(BigDecimal.valueOf(minPrice)) >= 0))
            .toList();
        int start = Math.min(page * size, filtered.size());
        int end = Math.min(start + size, filtered.size());
        List<ListingDTO> content = filtered.subList(start, end).stream().map(l -> toDTO(l, current)).toList();
        return new PageImpl<>(content, PageRequest.of(page, size), filtered.size());
    }

    public ListingDTO getById(UUID id, User current) {
        Listing l = listingRepository.findById(id).orElseThrow(() -> new RuntimeException("listing_not_found"));
        return toDTO(l, current);
    }

    public ListingDTO create(User owner, CreateListingRequest req) {
        Listing l = Listing.builder().id(UUID.randomUUID()).title(req.getTitle()).description(req.getDescription()).category(req.getCategory()).type(req.getType()).imageUrl(req.getImageUrl()).gallery(req.getGallery()).hourlyRate(req.getHourlyRate()).autoApprove(req.isAutoApprove()).status(AvailabilityStatus.AVAILABLE).location(Location.builder().lat(req.getX()).lng(req.getY()).build()).owner(owner).borrower(null).build();
        listingRepository.save(l);
        return toDTO(l, owner);
    }

    public ListingDTO update(UUID id, CreateListingRequest req, User current) {
        Listing l = listingRepository.findById(id).orElseThrow(() -> new RuntimeException("listing_not_found"));
        l.setTitle(req.getTitle());
        l.setDescription(req.getDescription());
        l.setCategory(req.getCategory());
        l.setType(req.getType());
        l.setImageUrl(req.getImageUrl());
        l.setGallery(req.getGallery());
        l.setHourlyRate(req.getHourlyRate());
        l.setAutoApprove(req.isAutoApprove());
        l.setLocation(Location.builder().lat(req.getX()).lng(req.getY()).build());
        listingRepository.save(l);
        return toDTO(l, current);
    }

    public void delete(UUID id) {
        listingRepository.deleteById(id);
    }

    public ListingDTO borrow(UUID id, User borrower, com.nearshare.api.dto.BorrowRequest request) {
        Listing l = listingRepository.findById(id).orElseThrow(() -> new RuntimeException("listing_not_found"));
        
        // Calculate amount
        BigDecimal amount = BigDecimal.ZERO;
        if (l.getHourlyRate().compareTo(BigDecimal.ZERO) > 0) {
            int duration = request.getDurationHours() > 0 ? request.getDurationHours() : 1;
            amount = l.getHourlyRate().multiply(BigDecimal.valueOf(duration));
            
            // Add service fee (e.g. 5%)
            BigDecimal serviceFee = amount.multiply(new BigDecimal("0.05")).setScale(2, java.math.RoundingMode.HALF_UP);
            amount = amount.add(serviceFee);
        }

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
             com.nearshare.api.model.Transaction t = com.nearshare.api.model.Transaction.builder()
                 .id(UUID.randomUUID())
                 .listing(l)
                 .payer(borrower)
                 .payee(l.getOwner())
                 .amount(amount)
                 .currency("USD")
                 .paymentMethod(request.getPaymentMethod())
                 .paymentToken(request.getPaymentToken())
                 .timestamp(java.time.LocalDateTime.now())
                 .status("SUCCESS")
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
                 .currency("USD")
                 .paymentMethod("CASH")
                 .timestamp(java.time.LocalDateTime.now())
                 .status("PENDING")
                 .build();
             transactionRepository.save(t);
        }

        l.setBorrower(borrower);
        if (l.isAutoApprove()) l.setStatus(AvailabilityStatus.BORROWED); else l.setStatus(AvailabilityStatus.PENDING);
        listingRepository.save(l);
        return toDTO(l, borrower);
    }

    public ListingDTO approve(UUID id, User owner) {
        Listing l = listingRepository.findById(id).orElseThrow(() -> new RuntimeException("listing_not_found"));
        l.setStatus(AvailabilityStatus.BORROWED);
        listingRepository.save(l);
        return toDTO(l, owner);
    }

    public ListingDTO deny(UUID id, User owner) {
        Listing l = listingRepository.findById(id).orElseThrow(() -> new RuntimeException("listing_not_found"));
        l.setStatus(AvailabilityStatus.AVAILABLE);
        l.setBorrower(null);
        listingRepository.save(l);
        return toDTO(l, owner);
    }

    public ListingDTO returnItem(UUID id, User owner) {
        Listing l = listingRepository.findById(id).orElseThrow(() -> new RuntimeException("listing_not_found"));
        l.setStatus(AvailabilityStatus.AVAILABLE);
        l.setBorrower(null);
        listingRepository.save(l);
        return toDTO(l, owner);
    }

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
                .filter(l -> l.getOwner() != null && !l.getOwner().getId().equals(current.getId()))
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
        double dist = 0;
        if (current != null && current.getLocation() != null && l.getLocation() != null && current.getLocation().getLat() != null && current.getLocation().getLng() != null && l.getLocation().getLat() != null && l.getLocation().getLng() != null) {
            dist = DistanceUtil.haversineMiles(current.getLocation().getLat(), current.getLocation().getLng(), l.getLocation().getLat(), l.getLocation().getLng());
        }
        return ListingDTO.builder()
            .id(l.getId())
            .ownerId(l.getOwner() != null ? l.getOwner().getId() : null)
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
            .build();
    }

    @org.springframework.transaction.annotation.Transactional
    public void completeTransaction(String paymentToken, String listingIdStr, String borrowerIdStr, BigDecimal amount, int durationHours) {
        if (transactionRepository.findByPaymentToken(paymentToken).isPresent()) {
            return;
        }

        UUID listingId = UUID.fromString(listingIdStr);
        UUID borrowerId = UUID.fromString(borrowerIdStr);

        Listing l = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("listing_not_found"));
        
        User borrower = userRepository.findById(borrowerId)
                .orElseThrow(() -> new RuntimeException("borrower_not_found"));

        com.nearshare.api.model.Transaction t = com.nearshare.api.model.Transaction.builder()
                .id(UUID.randomUUID())
                .listing(l)
                .payer(borrower)
                .payee(l.getOwner())
                .amount(amount)
                .currency("USD")
                .paymentMethod("CARD")
                .paymentToken(paymentToken)
                .timestamp(java.time.LocalDateTime.now())
                .status("SUCCESS")
                .build();
        transactionRepository.save(t);

        l.setBorrower(borrower);
        if (l.isAutoApprove()) l.setStatus(AvailabilityStatus.BORROWED); else l.setStatus(AvailabilityStatus.PENDING);
        listingRepository.save(l);
    }
}