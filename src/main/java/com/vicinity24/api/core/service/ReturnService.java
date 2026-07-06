package com.vicinity24.api.core.service;

import com.vicinity24.api.core.config.RuntimeSettingsService;
import com.vicinity24.api.core.dto.ReturnDTOs;
import com.vicinity24.api.core.model.Listing;
import com.vicinity24.api.core.model.ReturnSession;
import com.vicinity24.api.core.model.User;
import com.vicinity24.api.core.model.enums.AvailabilityStatus;
import com.vicinity24.api.core.model.enums.ReturnStatus;
import com.vicinity24.api.core.model.enums.ReturnMode;
import com.vicinity24.api.core.partner.model.PartnerAdminRole;
import com.vicinity24.api.core.partner.repository.PartnerAdminRepository;
import com.vicinity24.api.core.repository.ListingRepository;
import com.vicinity24.api.core.repository.ReturnSessionRepository;
import com.vicinity24.api.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final ReturnSessionRepository returnSessionRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final RuntimeSettingsService runtimeSettingsService;
    private final EscrowService escrowService;
    private final ReviewInviteService reviewInviteService;
    private final PartnerAdminRepository partnerAdminRepository;
    private final Random codeRandom = new java.security.SecureRandom();

    @Transactional
    public ReturnDTOs.ReturnSessionResponse initiateReturn(UUID listingId, User currentUser) {
        if (!anyReturnMethodEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Return methods are disabled");
        }
        Listing listing = getBorrowedListing(listingId);
        ReturnDTOs.SubmitReturnRequest request = new ReturnDTOs.SubmitReturnRequest();
        if (manualEnabled()) {
            request.setReturnMethod(ReturnMode.MANUAL);
            request.setItemNumber(listing.getItemReference());
        } else {
            request.setReturnMethod(ReturnMode.QR_CODE);
            request.setQrCode(generateSixDigitCode());
        }
        return submitReturn(listingId, currentUser, request);
    }

    @Transactional
    public ReturnDTOs.ReturnSessionResponse submitReturn(UUID listingId, User currentUser, ReturnDTOs.SubmitReturnRequest request) {
        Listing listing = getBorrowedListing(listingId);
        if (!isBorrower(listing, currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the borrower can submit a return request");
        }

        ReturnMode method = resolveRequestedMethod(request);
        if (method == ReturnMode.QR_CODE && !qrEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "QR return method is disabled");
        }
        if (method == ReturnMode.MANUAL && !manualEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manual return method is disabled");
        }

        User lender = resolveLender(listing);
        if (lender == null || lender.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Listing is missing lender");
        }

        ReturnSession session = getOrCreatePendingSession(listing, lender);
        LocalDateTime now = LocalDateTime.now();
        String qrCode = sanitize(request != null ? request.getQrCode() : null);
        String itemNumber = sanitize(request != null ? request.getItemNumber() : null);

        if (method == ReturnMode.QR_CODE) {
            if (qrCode == null || !qrCode.matches("\\d{6}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid QR code");
            }
            session.setBorrowerCode(qrCode);
            session.setBorrowerScannedAt(now);
            session.setManualBorrowerConfirmedAt(null);
            session.setManualLenderConfirmedAt(null);
        } else {
            validateItemReference(listing, itemNumber);
            session.setBorrowerCode(null);
            session.setBorrowerScannedAt(null);
            session.setLenderScannedAt(null);
            session.setManualBorrowerConfirmedAt(now);
            session.setManualLenderConfirmedAt(null);
        }

        session.setReturnMethod(method);
        session.setBorrower(listing.getBorrower());
        session.setLender(lender);
        session.setReturnPlace(sanitize(request != null ? request.getReturnPlace() : null));
        session.setReturnAddress(sanitize(request != null ? request.getReturnAddress() : null));
        session.setSubmittedAt(now);
        session.setAcceptedAt(null);
        session.setConciergeWitnessId(null);
        session.setDisputeReason(null);
        session.setDisputePhotoUrl(null);
        session.setStatus(ReturnStatus.PENDING);
        session.setExpiresAt(null);

        listing.setStatus(AvailabilityStatus.WAITING_FOR_RETURN);
        listingRepository.save(listing);

        return mapToResponse(returnSessionRepository.save(session));
    }

    @Transactional
    public ReturnDTOs.ReturnSessionResponse scanQrCode(UUID listingId, User currentUser, String qrCode) {
        ReturnDTOs.SubmitReturnRequest request = new ReturnDTOs.SubmitReturnRequest();
        request.setReturnMethod(ReturnMode.QR_CODE);
        request.setQrCode(qrCode);
        return submitReturn(listingId, currentUser, request);
    }

    @Transactional
    public ReturnDTOs.ReturnSessionResponse manualFallback(UUID listingId, User currentUser, ReturnDTOs.ManualFallbackRequest request) {
        ReturnSession pending = findPendingSession(listingId);
        if (pending != null && canActAsLender(pending.getListing(), currentUser)) {
            validateItemReference(pending.getListing(), request != null ? request.getItemNumber() : null);
            if (request != null && request.getConciergeWitnessId() != null && !request.getConciergeWitnessId().isBlank()) {
                pending.setConciergeWitnessId(request.getConciergeWitnessId().trim());
                returnSessionRepository.save(pending);
            }
            return acceptReturn(listingId, currentUser);
        }

        ReturnDTOs.SubmitReturnRequest submitRequest = new ReturnDTOs.SubmitReturnRequest();
        submitRequest.setReturnMethod(ReturnMode.MANUAL);
        submitRequest.setItemNumber(request != null ? request.getItemNumber() : null);
        submitRequest.setReturnPlace(request != null ? request.getReturnPlace() : null);
        submitRequest.setReturnAddress(request != null ? request.getReturnAddress() : null);
        return submitReturn(listingId, currentUser, submitRequest);
    }

    @Transactional
    public ReturnDTOs.ReturnSessionResponse acceptReturn(UUID listingId, User currentUser) {
        ReturnSession session = getPendingSessionForLender(listingId, currentUser);
        Listing listing = session.getListing();
        LocalDateTime now = LocalDateTime.now();

        session.setStatus(ReturnStatus.COMPLETED);
        session.setAcceptedAt(now);
        session.setExpiresAt(now);
        if (session.getReturnMethod() == ReturnMode.MANUAL) {
            session.setManualLenderConfirmedAt(now);
        }
        if (session.getReturnMethod() == ReturnMode.QR_CODE) {
            session.setLenderScannedAt(now);
        }

        listing.setStatus(listing.getPartner() != null ? AvailabilityStatus.PARTNER_ACTIVE : AvailabilityStatus.AVAILABLE);
        listing.setBorrower(null);
        listing.setItemReference(null);
        listing.setAdminReturnRequestedAt(null);
        listing.setAdminReturnRequestedBy(null);
        listing.setAdminReturnRequestReason(null);

        User lender = session.getLender();
        User borrower = session.getBorrower();
        if (lender != null) {
            lender.setTrustScore(Math.min(100, lender.getTrustScore() + 1));
            userRepository.save(lender);
        }
        if (borrower != null) {
            borrower.setTrustScore(Math.min(100, borrower.getTrustScore() + 1));
            userRepository.save(borrower);
        }

        listingRepository.save(listing);
        ReturnSession saved = returnSessionRepository.save(session);
        escrowService.releaseOnSuccessfulReturn(saved);
        try {
            reviewInviteService.createAndSendForReturnSession(saved);
        } catch (Exception ignored) {
        }
        return mapToResponse(saved);
    }

    @Transactional
    public ReturnDTOs.ReturnSessionResponse denyManualReturn(UUID listingId, User currentUser, String reason) {
        ReturnDTOs.DisputeRequest request = new ReturnDTOs.DisputeRequest();
        request.setReason((reason != null && !reason.isBlank()) ? reason.trim() : "manual_return_denied");
        return initiateDispute(listingId, currentUser, request);
    }

    @Transactional
    public ReturnDTOs.ReturnSessionResponse initiateDispute(UUID listingId, User currentUser, ReturnDTOs.DisputeRequest request) {
        if (!disputeEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Dispute return method is disabled");
        }
        ReturnSession session = getPendingSessionForLender(listingId, currentUser);
        if (currentUser == null || currentUser.getId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not part of this return");
        }

        session.setStatus(ReturnStatus.DISPUTED);
        session.setDisputeReason(sanitize(request != null ? request.getReason() : null));
        session.setDisputePhotoUrl(sanitize(request != null ? request.getPhotoUrl() : null));
        if (request != null && request.getConciergeWitnessId() != null) {
            session.setConciergeWitnessId(request.getConciergeWitnessId());
        }
        session.setExpiresAt(LocalDateTime.now());

        Listing listing = session.getListing();
        listing.setStatus(AvailabilityStatus.DISPUTED);
        listingRepository.save(listing);
        escrowService.markDisputed(listingId, session.getDisputeReason() != null ? session.getDisputeReason() : "dispute");

        return mapToResponse(returnSessionRepository.save(session));
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkExpiredSessions() {
        // Timed mutual-return expiry is intentionally disabled in the simplified flow.
    }

    private ReturnDTOs.ReturnSessionResponse mapToResponse(ReturnSession session) {
        Listing listing = session.getListing();
        User borrower = session.getBorrower() != null ? session.getBorrower() : (listing != null ? listing.getBorrower() : null);
        User lender = session.getLender() != null ? session.getLender() : (listing != null ? listing.getOwner() : null);
        return ReturnDTOs.ReturnSessionResponse.builder()
                .id(session.getId())
                .listingId(listing != null ? listing.getId() : null)
                .borrowerName(displayName(borrower))
                .lenderName(displayName(lender))
                .itemReference(listing != null ? listing.getItemReference() : null)
                .returnMethod(session.getReturnMethod())
                .returnPlace(session.getReturnPlace())
                .returnAddress(session.getReturnAddress())
                .submittedAt(session.getSubmittedAt())
                .acceptedAt(session.getAcceptedAt())
                .disputeReason(session.getDisputeReason())
                .borrowerCode(session.getBorrowerCode())
                .lenderCode(session.getLenderCode())
                .borrowerScanned(session.getBorrowerScannedAt() != null)
                .lenderScanned(session.getLenderScannedAt() != null)
                .manualBorrowerConfirmed(session.getManualBorrowerConfirmedAt() != null)
                .manualLenderConfirmed(session.getManualLenderConfirmedAt() != null)
                .status(session.getStatus())
                .expiresAt(session.getExpiresAt())
                .build();
    }

    private String generateSixDigitCode() {
        int value = codeRandom.nextInt(900000) + 100000;
        return String.valueOf(value);
    }

    private String generateUniqueItemReference() {
        for (int attempt = 0; attempt < 25; attempt++) {
            int v = codeRandom.nextInt(100_000_000);
            String code = String.format("%08d", v);
            if (!listingRepository.existsByItemReference(code)) {
                return code;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed_to_generate_item_reference");
    }

    @Transactional(readOnly = true)
    public ReturnDTOs.ReturnSessionResponse getSession(UUID listingId, User currentUser) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));

        boolean listingActive = listing.getStatus() == AvailabilityStatus.BORROWED
                || listing.getStatus() == AvailabilityStatus.WAITING_FOR_RETURN
                || listing.getStatus() == AvailabilityStatus.DISPUTED;
        ReturnSession session = listingActive
                ? returnSessionRepository.findFirstByListingIdAndStatusOrderByCreatedAtDesc(listingId, ReturnStatus.PENDING).orElse(null)
                : returnSessionRepository.findFirstByListingIdOrderByCreatedAtDesc(listingId).orElse(null);
        if (currentUser == null || currentUser.getId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not part of this return");
        }
        if (session == null) {
            if (listingActive && (isBorrower(listing, currentUser) || canActAsLender(listing, currentUser))) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No return session");
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No return session");
        }
        UUID currentId = currentUser.getId();
        UUID borrowerId = session.getBorrower() != null ? session.getBorrower().getId() : null;
        UUID lenderId = session.getLender() != null ? session.getLender().getId() : null;
        if (!currentId.equals(borrowerId) && !currentId.equals(lenderId) && !canActAsLender(listing, currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not part of this return");
        }
        return mapToResponse(session);
    }

    private ReturnSession getOrCreatePendingSession(Listing listing, User lender) {
        UUID listingId = listing.getId();
        List<ReturnSession> pending = returnSessionRepository.findByListingIdAndStatusOrderByCreatedAtDesc(listingId, ReturnStatus.PENDING);
        if (!pending.isEmpty()) {
            ReturnSession active = pending.get(0);
            for (int i = 1; i < pending.size(); i++) {
                ReturnSession extra = pending.get(i);
                extra.setStatus(ReturnStatus.DISPUTED);
                extra.setDisputeReason("duplicate_pending_return_cleanup");
                returnSessionRepository.save(extra);
            }
            return active;
        }
        return ReturnSession.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .borrower(listing.getBorrower())
                .lender(lender)
                .borrowerQrCode(null)
                .lenderQrCode(null)
                .borrowerCode(null)
                .lenderCode(null)
                .status(ReturnStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .expiresAt(null)
                .build();
    }

    private User resolveLender(Listing listing) {
        if (listing.getOwner() != null) {
            return listing.getOwner();
        }
        if (listing.getPartner() == null || listing.getPartner().getId() == null) {
            return null;
        }
        var admins = partnerAdminRepository.findUsersByPartnerIdAndRoleOrderByCreatedAtAsc(listing.getPartner().getId(), PartnerAdminRole.ADMIN);
        if (!admins.isEmpty()) {
            return admins.get(0);
        }
        return null;
    }

    private ReturnSession getPendingSessionForLender(UUID listingId, User currentUser) {
        ReturnSession session = findPendingSession(listingId);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No pending return request");
        }
        if (!canActAsLender(session.getListing(), currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not part of this return");
        }
        List<ReturnSession> pending = returnSessionRepository.findByListingIdAndStatusOrderByCreatedAtDesc(listingId, ReturnStatus.PENDING);
        for (int i = 1; i < pending.size(); i++) {
            ReturnSession extra = pending.get(i);
            extra.setStatus(ReturnStatus.DISPUTED);
            extra.setDisputeReason("duplicate_pending_return_cleanup");
            returnSessionRepository.save(extra);
        }
        return session;
    }

    private ReturnSession findPendingSession(UUID listingId) {
        return returnSessionRepository.findFirstByListingIdAndStatusOrderByCreatedAtDesc(listingId, ReturnStatus.PENDING)
                .orElse(null);
    }

    private Listing getBorrowedListing(UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        AvailabilityStatus status = listing.getStatus();
        if (status == AvailabilityStatus.AVAILABLE && listing.getBorrower() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Listing already returned");
        }
        if (status != AvailabilityStatus.BORROWED && status != AvailabilityStatus.WAITING_FOR_RETURN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Listing is not currently borrowed");
        }
        if (listing.getBorrower() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Listing is missing borrower");
        }
        if (listing.getItemReference() == null || listing.getItemReference().isBlank()) {
            listing.setItemReference(generateUniqueItemReference());
            listingRepository.save(listing);
        }
        return listing;
    }

    private boolean anyReturnMethodEnabled() {
        return qrEnabled() || manualEnabled();
    }

    private boolean qrEnabled() {
        String rawMode = String.valueOf(runtimeSettingsService != null ? runtimeSettingsService.getValue("settings.returns.mode") : "");
        ReturnMode mode = ReturnMode.from(rawMode);
        if (mode != ReturnMode.ANY) return mode == ReturnMode.QR_CODE;
        return runtimeSettingsService == null || runtimeSettingsService.isEnabled("settings.returns.qr.enabled", true);
    }

    private boolean manualEnabled() {
        String rawMode = String.valueOf(runtimeSettingsService != null ? runtimeSettingsService.getValue("settings.returns.mode") : "");
        ReturnMode mode = ReturnMode.from(rawMode);
        if (mode != ReturnMode.ANY) return mode == ReturnMode.MANUAL;
        return runtimeSettingsService == null || runtimeSettingsService.isEnabled("settings.returns.manual.enabled", true);
    }

    private boolean disputeEnabled() {
        String rawMode = String.valueOf(runtimeSettingsService != null ? runtimeSettingsService.getValue("settings.returns.mode") : "");
        ReturnMode mode = ReturnMode.from(rawMode);
        if (mode != ReturnMode.ANY) return mode == ReturnMode.DISPUTE;
        return runtimeSettingsService == null || runtimeSettingsService.isEnabled("settings.returns.dispute.enabled", true);
    }

    private ReturnMode resolveRequestedMethod(ReturnDTOs.SubmitReturnRequest request) {
        ReturnMode method = request != null ? request.getReturnMethod() : null;
        if (method == null || method == ReturnMode.ANY || method == ReturnMode.DISPUTE) {
            String qrCode = sanitize(request != null ? request.getQrCode() : null);
            method = qrCode != null ? ReturnMode.QR_CODE : ReturnMode.MANUAL;
        }
        return method;
    }

    private boolean isBorrower(Listing listing, User currentUser) {
        return listing != null
                && listing.getBorrower() != null
                && listing.getBorrower().getId() != null
                && currentUser != null
                && currentUser.getId() != null
                && listing.getBorrower().getId().equals(currentUser.getId());
    }

    private boolean canActAsLender(Listing listing, User currentUser) {
        if (listing == null || currentUser == null || currentUser.getId() == null) {
            return false;
        }
        if (currentUser.getRole() != null && currentUser.getRole().name().equals("ADMIN")) {
            return true;
        }
        User lender = resolveLender(listing);
        if (lender != null && lender.getId() != null && lender.getId().equals(currentUser.getId())) {
            return true;
        }
        return listing.getPartner() != null
                && listing.getPartner().getId() != null
                && partnerAdminRepository.existsByUserAndPartnerAndRole(currentUser.getId(), listing.getPartner().getId(), PartnerAdminRole.ADMIN);
    }

    private void validateItemReference(Listing listing, String provided) {
        String itemNumber = sanitize(provided);
        String expected = sanitize(listing != null ? listing.getItemReference() : null);
        if (expected != null) {
            if (itemNumber == null || !expected.equals(itemNumber)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid item number");
            }
            return;
        }
        if (itemNumber == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing item number");
        }
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String displayName(User user) {
        if (user == null) {
            return null;
        }
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        return user.getName();
    }
}
