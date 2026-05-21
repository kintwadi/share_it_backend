package com.nearshare.api.admin.service;

import com.nearshare.api.admin.dto.AdminDisputeDTO;
import com.nearshare.api.admin.dto.AdminListingDTO;
import com.nearshare.api.admin.dto.AdminPageResponse;
import com.nearshare.api.admin.dto.AdminSubscriptionDTO;
import com.nearshare.api.admin.dto.AdminSummaryDTO;
import com.nearshare.api.admin.dto.AdminTransactionDTO;
import com.nearshare.api.admin.dto.AdminUserDTO;
import com.nearshare.api.model.Listing;
import com.nearshare.api.model.ReturnSession;
import com.nearshare.api.model.Subscription;
import com.nearshare.api.model.Transaction;
import com.nearshare.api.model.User;
import com.nearshare.api.model.enums.AvailabilityStatus;
import com.nearshare.api.model.enums.ReturnStatus;
import com.nearshare.api.model.enums.UserStatus;
import com.nearshare.api.payment.StripePayment;
import com.nearshare.api.repository.ListingRepository;
import com.nearshare.api.repository.ReturnSessionRepository;
import com.nearshare.api.repository.SubscriptionRepository;
import com.nearshare.api.repository.TransactionRepository;
import com.nearshare.api.repository.UserRepository;
import com.nearshare.api.service.EscrowService;
import com.nearshare.api.service.UserService;
import com.stripe.model.Refund;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.random.RandomGenerator;

@Service
public class AdminManagementService {
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ListingRepository listingRepository;
    private final ReturnSessionRepository returnSessionRepository;
    private final StripePayment stripePayment;
    private final EscrowService escrowService;
    private final UserService userService;
    private final RandomGenerator codeRandom = new java.security.SecureRandom();

    public AdminManagementService(
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            SubscriptionRepository subscriptionRepository,
            ListingRepository listingRepository,
            ReturnSessionRepository returnSessionRepository,
            StripePayment stripePayment,
            EscrowService escrowService,
            UserService userService
    ) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.listingRepository = listingRepository;
        this.returnSessionRepository = returnSessionRepository;
        this.stripePayment = stripePayment;
        this.escrowService = escrowService;
        this.userService = userService;
    }

    @Transactional
    public AdminSummaryDTO getSummary() {
        return AdminSummaryDTO.builder()
                .users(userRepository.count())
                .transactions(transactionRepository.count())
                .subscriptions(subscriptionRepository.count())
                .disputedListings(listingRepository.countByStatus(AvailabilityStatus.DISPUTED))
                .disputedReturns(returnSessionRepository.countByStatus(ReturnStatus.DISPUTED))
                .releaseFailedTransactions(transactionRepository.countByStatus("RELEASE_FAILED"))
                .build();
    }

    @Transactional
    public AdminPageResponse<AdminUserDTO> listUsers(String q, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "joinedDate"));
        Page<User> p;
        if (q == null || q.isBlank()) {
            p = userRepository.findAll(pageable);
        } else {
            p = userRepository.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(q, q, pageable);
        }
        List<AdminUserDTO> items = p.getContent().stream().map(this::toAdminUserDTO).toList();
        return new AdminPageResponse<>(items, p.getTotalElements(), page, size);
    }

    @Transactional
    public AdminUserDTO setUserStatus(UUID userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setStatus(status);
        userRepository.save(user);
        return toAdminUserDTO(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        try {
            userService.deleteUser(userId);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete user with related data. Block the user instead.");
        }
    }

    @Transactional
    public AdminPageResponse<AdminTransactionDTO> listTransactions(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<Transaction> p = (status == null || status.isBlank())
                ? transactionRepository.findAll(pageable)
                : transactionRepository.findByStatus(status, pageable);
        List<AdminTransactionDTO> items = p.getContent().stream().map(this::toAdminTransactionDTO).toList();
        return new AdminPageResponse<>(items, p.getTotalElements(), page, size);
    }

    @Transactional
    public void deleteTransaction(UUID transactionId) {
        try {
            transactionRepository.deleteById(transactionId);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete transaction");
        }
    }

    @Transactional
    public boolean retryTransactionRelease(UUID transactionId) {
        if (transactionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing transactionId");
        }
        boolean attempted = escrowService.adminRetryReleaseForTransaction(transactionId);
        if (!attempted) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not eligible for retry");
        }
        return true;
    }

    @Transactional
    public AdminPageResponse<AdminSubscriptionDTO> listSubscriptions(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Subscription> p = (status == null || status.isBlank())
                ? subscriptionRepository.findAll(pageable)
                : subscriptionRepository.findByStatus(status, pageable);
        List<AdminSubscriptionDTO> items = p.getContent().stream().map(this::toAdminSubscriptionDTO).toList();
        return new AdminPageResponse<>(items, p.getTotalElements(), page, size);
    }

    @Transactional
    public AdminPageResponse<AdminListingDTO> listListings(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Listing> p;
        if (status == null || status.isBlank()) {
            p = listingRepository.findAll(pageable);
        } else {
            AvailabilityStatus s;
            try {
                s = AvailabilityStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
            }
            p = listingRepository.findByStatus(s, pageable);
        }
        List<AdminListingDTO> items = p.getContent().stream().map(this::toAdminListingDTO).toList();
        return new AdminPageResponse<>(items, p.getTotalElements(), page, size);
    }

    @Transactional
    public AdminPageResponse<AdminListingDTO> listPartnerListingRequests(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "partnerSubmittedAt", "createdAt"));
        Page<Listing> p = listingRepository.findByStatus(AvailabilityStatus.PARTNER_PENDING_APPROVAL, pageable);
        List<AdminListingDTO> items = p.getContent().stream().map(this::toAdminListingDTO).toList();
        return new AdminPageResponse<>(items, p.getTotalElements(), page, size);
    }

    @Transactional
    public AdminPageResponse<AdminDisputeDTO> listDisputes(int page, int size) {
        List<Listing> disputedListings = listingRepository.findByStatusOrderByCreatedAtDesc(AvailabilityStatus.DISPUTED);
        List<ReturnSession> disputedReturns = returnSessionRepository.findByStatusOrderByCreatedAtDesc(ReturnStatus.DISPUTED);

        Map<UUID, AdminDisputeDTO.AdminDisputeDTOBuilder> merged = new HashMap<>();

        for (Listing l : disputedListings) {
            if (l == null || l.getId() == null) continue;
            merged.put(l.getId(), baseDispute(l));
        }

        for (ReturnSession rs : disputedReturns) {
            if (rs == null || rs.getListing() == null || rs.getListing().getId() == null) continue;
            UUID listingId = rs.getListing().getId();
            AdminDisputeDTO.AdminDisputeDTOBuilder b = merged.getOrDefault(listingId, baseDispute(rs.getListing()));
            b.returnSessionId(rs.getId())
                    .returnStatus(rs.getStatus())
                    .disputeReason(rs.getDisputeReason());
            if (b.build().getCreatedAt() == null || (rs.getCreatedAt() != null && rs.getCreatedAt().isAfter(b.build().getCreatedAt()))) {
                b.createdAt(rs.getCreatedAt());
            }
            merged.put(listingId, b);
        }

        List<AdminDisputeDTO> all = new ArrayList<>();
        for (Map.Entry<UUID, AdminDisputeDTO.AdminDisputeDTOBuilder> e : merged.entrySet()) {
            UUID listingId = e.getKey();
            AdminDisputeDTO.AdminDisputeDTOBuilder b = e.getValue();
            Transaction tx = transactionRepository.findFirstByListingIdOrderByTimestampDesc(listingId).orElse(null);
            if (tx != null) {
                b.latestTransactionId(tx.getId())
                        .latestTransactionStatus(tx.getStatus())
                        .latestReleaseError(tx.getReleaseError());
            }
            all.add(b.build());
        }

        all.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return new AdminPageResponse<>(all.subList(from, to), all.size(), page, size);
    }

    @Transactional
    public void blockListing(UUID listingId, boolean blocked) {
        Listing l = listingRepository.findById(listingId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        l.setStatus(blocked ? AvailabilityStatus.BLOCKED : AvailabilityStatus.AVAILABLE);
        if (!blocked) {
            l.setBorrower(null);
        }
        listingRepository.save(l);
    }

    @Transactional
    public void deleteListing(UUID listingId) {
        try {
            returnSessionRepository.deleteByListingId(listingId);
            List<Transaction> txs = transactionRepository.findByListingId(listingId);
            if (!txs.isEmpty()) {
                transactionRepository.deleteAll(txs);
            }
            listingRepository.deleteById(listingId);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete listing");
        }
    }

    @Transactional
    public void approvePartnerListing(User admin, UUID listingId, String note) {
        Listing l = listingRepository.findById(listingId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (l.getPartner() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a partner listing");
        }
        if (l.getStatus() != AvailabilityStatus.PARTNER_PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid status");
        }
        l.setStatus(AvailabilityStatus.AVAILABLE);
        l.setPartnerReviewedAt(java.time.LocalDateTime.now());
        l.setPartnerReviewedBy(admin != null ? admin.getId() : null);
        if (note != null && !note.isBlank()) {
            String n = note.trim();
            if (n.length() > 500) n = n.substring(0, 500);
            l.setPartnerReviewNote(n);
        } else {
            l.setPartnerReviewNote(null);
        }
        l.setPartnerRejectionReason(null);
        listingRepository.save(l);
    }

    @Transactional
    public void rejectPartnerListing(User admin, UUID listingId, String reason) {
        Listing l = listingRepository.findById(listingId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (l.getPartner() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a partner listing");
        }
        if (l.getStatus() != AvailabilityStatus.PARTNER_PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid status");
        }
        l.setStatus(AvailabilityStatus.BLOCKED);
        l.setPartnerReviewedAt(java.time.LocalDateTime.now());
        l.setPartnerReviewedBy(admin != null ? admin.getId() : null);
        if (reason != null && !reason.isBlank()) {
            String r = reason.trim();
            if (r.length() > 500) r = r.substring(0, 500);
            l.setPartnerRejectionReason(r);
        } else {
            l.setPartnerRejectionReason(null);
        }
        l.setPartnerReviewNote(null);
        listingRepository.save(l);
    }

    @Transactional
    public void cancelAndRefundDispute(UUID listingId, String reason) {
        Listing l = listingRepository.findById(listingId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));

        Transaction tx = transactionRepository.findFirstByListingIdOrderByTimestampDesc(listingId).orElse(null);
        if (tx != null && tx.getStripeRefundId() == null && tx.getPaymentToken() != null && "CARD".equalsIgnoreCase(tx.getPaymentMethod())) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("action", "admin_cancel_refund");
            if (reason != null && !reason.isBlank()) {
                metadata.put("reason", reason);
            }
            Refund refund = stripePayment.createRefund(tx.getPaymentToken(), tx.getAmount(), metadata);
            tx.setStripeRefundId(refund.getId());
            tx.setStatus("REFUNDED");
            tx.setReleasedAt(LocalDateTime.now());
            tx.setReleaseError(reason);
            transactionRepository.save(tx);
        }

        l.setBorrower(null);
        l.setStatus(AvailabilityStatus.AVAILABLE);
        listingRepository.save(l);

        for (ReturnSession rs : returnSessionRepository.findByListingIdAndStatusOrderByCreatedAtDesc(listingId, ReturnStatus.DISPUTED)) {
            rs.setStatus(ReturnStatus.COMPLETED);
        }
    }

    @Transactional
    public void acceptReturnDispute(UUID listingId, String reason) {
        Listing l = listingRepository.findById(listingId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        ReturnSession session = returnSessionRepository.findFirstByListingIdOrderByCreatedAtDesc(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No return session"));

        session.setStatus(ReturnStatus.COMPLETED);
        if (reason != null && !reason.isBlank()) {
            session.setDisputeReason(reason);
        }
        session.setExpiresAt(LocalDateTime.now());
        returnSessionRepository.save(session);

        l.setStatus(AvailabilityStatus.AVAILABLE);
        l.setBorrower(null);
        listingRepository.save(l);

        escrowService.adminAttemptReleaseForListing(listingId);
    }

    @Transactional
    public ReturnSession reopenReturnSession(UUID listingId, Integer minutes) {
        Listing l = listingRepository.findById(listingId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (l.getOwner() == null || l.getBorrower() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Listing is missing borrower/owner");
        }

        for (ReturnSession rs : returnSessionRepository.findByListingIdAndStatusOrderByCreatedAtDesc(listingId, ReturnStatus.PENDING)) {
            rs.setStatus(ReturnStatus.DISPUTED);
            rs.setDisputeReason("admin_reopen_cleanup");
            returnSessionRepository.save(rs);
        }

        l.setStatus(AvailabilityStatus.BORROWED);
        listingRepository.save(l);

        int m = minutes != null && minutes > 0 && minutes <= 60 ? minutes : 5;
        String borrowerCode = generateSixDigitCode();
        String lenderCode = generateSixDigitCode();
        while (lenderCode.equals(borrowerCode)) {
            lenderCode = generateSixDigitCode();
        }
        ReturnSession created = ReturnSession.builder()
                .id(UUID.randomUUID())
                .listing(l)
                .borrower(l.getBorrower())
                .lender(l.getOwner())
                .borrowerQrCode("BQR-" + UUID.randomUUID())
                .lenderQrCode("LQR-" + UUID.randomUUID())
                .borrowerCode(borrowerCode)
                .lenderCode(lenderCode)
                .status(ReturnStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(m))
                .build();
        return returnSessionRepository.save(created);
    }

    private String generateSixDigitCode() {
        int value = codeRandom.nextInt(900000) + 100000;
        return String.valueOf(value);
    }

    private AdminUserDTO toAdminUserDTO(User u) {
        return AdminUserDTO.builder()
                .id(u.getId())
                .name(u.getName())
                .displayName(u.getDisplayName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .address(u.getAddress())
                .role(u.getRole())
                .status(u.getStatus())
                .verificationStatus(u.getVerificationStatus())
                .trustScore(u.getTrustScore())
                .vouchCount(u.getVouchCount())
                .joinedDate(u.getJoinedDate())
                .twoFactorEnabled(Boolean.TRUE.equals(u.getTwoFactorEnabled()))
                .build();
    }

    private AdminTransactionDTO toAdminTransactionDTO(Transaction t) {
        return AdminTransactionDTO.builder()
                .id(t.getId())
                .listingId(t.getListing() != null ? t.getListing().getId() : null)
                .listingTitle(t.getListing() != null ? t.getListing().getTitle() : null)
                .payerId(t.getPayer() != null ? t.getPayer().getId() : null)
                .payerEmail(t.getPayer() != null ? t.getPayer().getEmail() : null)
                .payeeId(t.getPayee() != null ? t.getPayee().getId() : null)
                .payeeEmail(t.getPayee() != null ? t.getPayee().getEmail() : null)
                .amount(t.getAmount())
                .rentalAmount(t.getRentalAmount())
                .serviceFeeAmount(t.getServiceFeeAmount())
                .depositAmount(t.getDepositAmount())
                .currency(t.getCurrency())
                .paymentMethod(t.getPaymentMethod())
                .status(t.getStatus())
                .paymentToken(t.getPaymentToken())
                .stripeTransferId(t.getStripeTransferId())
                .stripeRefundId(t.getStripeRefundId())
                .releaseError(t.getReleaseError())
                .timestamp(t.getTimestamp())
                .build();
    }

    private AdminSubscriptionDTO toAdminSubscriptionDTO(Subscription s) {
        return AdminSubscriptionDTO.builder()
                .id(s.getId())
                .userId(s.getUser() != null ? s.getUser().getId() : null)
                .userEmail(s.getUser() != null ? s.getUser().getEmail() : null)
                .planType(s.getPlanType())
                .status(s.getStatus())
                .trialStart(s.getTrialStart())
                .trialEnd(s.getTrialEnd())
                .autoChargeAmountCents(s.getAutoChargeAmountCents())
                .autoChargeDate(s.getAutoChargeDate())
                .createdAt(s.getCreatedAt())
                .stripeSubscriptionId(s.getStripeSubscriptionId())
                .build();
    }

    private AdminListingDTO toAdminListingDTO(Listing l) {
        return AdminListingDTO.builder()
                .id(l.getId())
                .title(l.getTitle())
                .type(l.getType())
                .status(l.getStatus())
                .hourlyRate(l.getHourlyRate())
                .ownerId(l.getOwner() != null ? l.getOwner().getId() : null)
                .ownerEmail(l.getOwner() != null ? l.getOwner().getEmail() : null)
                .partnerId(l.getPartner() != null ? l.getPartner().getId() : null)
                .partnerName(l.getPartner() != null ? l.getPartner().getName() : null)
                .borrowerId(l.getBorrower() != null ? l.getBorrower().getId() : null)
                .borrowerEmail(l.getBorrower() != null ? l.getBorrower().getEmail() : null)
                .createdAt(l.getCreatedAt())
                .availableUnlimited(l.isAvailableUnlimited())
                .availableFrom(l.getAvailableFrom())
                .availableTo(l.getAvailableTo())
                .partnerSubmittedAt(l.getPartnerSubmittedAt())
                .partnerSubmittedBy(l.getPartnerSubmittedBy())
                .partnerReviewedAt(l.getPartnerReviewedAt())
                .partnerReviewedBy(l.getPartnerReviewedBy())
                .partnerReviewNote(l.getPartnerReviewNote())
                .partnerRejectionReason(l.getPartnerRejectionReason())
                .build();
    }

    private AdminDisputeDTO.AdminDisputeDTOBuilder baseDispute(Listing l) {
        return AdminDisputeDTO.builder()
                .listingId(l.getId())
                .listingTitle(l.getTitle())
                .listingStatus(l.getStatus())
                .ownerId(l.getOwner() != null ? l.getOwner().getId() : null)
                .ownerEmail(l.getOwner() != null ? l.getOwner().getEmail() : null)
                .borrowerId(l.getBorrower() != null ? l.getBorrower().getId() : null)
                .borrowerEmail(l.getBorrower() != null ? l.getBorrower().getEmail() : null)
                .createdAt(l.getCreatedAt());
    }
}
