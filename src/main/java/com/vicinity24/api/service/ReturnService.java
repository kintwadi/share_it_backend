package com.vicinity24.api.service;

import com.vicinity24.api.config.RuntimeSettingsService;
import com.vicinity24.api.dto.ReturnDTOs;
import com.vicinity24.api.model.Listing;
import com.vicinity24.api.model.ReturnSession;
import com.vicinity24.api.model.User;
import com.vicinity24.api.model.enums.AvailabilityStatus;
import com.vicinity24.api.model.enums.ReturnStatus;
import com.vicinity24.api.model.enums.ReturnMode;
import com.vicinity24.api.partner.model.PartnerAdminRole;
import com.vicinity24.api.partner.repository.PartnerAdminRepository;
import com.vicinity24.api.repository.ListingRepository;
import com.vicinity24.api.repository.ReturnSessionRepository;
import com.vicinity24.api.repository.UserRepository;
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
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));

        if (listing.getStatus() != AvailabilityStatus.BORROWED && listing.getStatus() != AvailabilityStatus.WAITING_FOR_RETURN && listing.getStatus() != AvailabilityStatus.PARTNER_ACTIVE) {
            if (listing.getStatus() == AvailabilityStatus.AVAILABLE && listing.getBorrower() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Listing already returned");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Listing is not currently borrowed");
        }

        if (listing.getBorrower() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Listing is missing borrower");
        }
        if ((listing.getStatus() == AvailabilityStatus.BORROWED || listing.getStatus() == AvailabilityStatus.WAITING_FOR_RETURN)
                && (listing.getItemReference() == null || listing.getItemReference().isBlank())) {
            listing.setItemReference(generateUniqueItemReference());
            listingRepository.save(listing);
        }

        User lender = resolveLender(listing);
        if (lender == null || lender.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Listing is missing lender");
        }

        boolean isBorrower = currentUser != null && currentUser.getId() != null && currentUser.getId().equals(listing.getBorrower().getId());
        boolean isLender = currentUser != null && currentUser.getId() != null && currentUser.getId().equals(lender.getId());
        if (!isBorrower && !isLender) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not part of this return");
        }

        ReturnSession session = getOrCreateActiveSession(listing, lender);
        ensureCodes(session);
        return mapToResponse(returnSessionRepository.save(session));
    }

    @Transactional
    public ReturnDTOs.ReturnSessionResponse scanQrCode(UUID listingId, User currentUser, String qrCode) {
        if (!qrEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "QR return method is disabled");
        }
        ReturnSession session = getActiveSession(listingId);

        boolean borrowerScanningLender = currentUser.getId().equals(session.getBorrower().getId())
                && (qrCode.equals(session.getLenderCode()) || qrCode.equals(session.getLenderQrCode()));
        boolean lenderScanningBorrower = currentUser.getId().equals(session.getLender().getId())
                && (qrCode.equals(session.getBorrowerCode()) || qrCode.equals(session.getBorrowerQrCode()));

        if (borrowerScanningLender) {
            session.setBorrowerScannedAt(LocalDateTime.now());
        } else if (lenderScanningBorrower) {
            session.setLenderScannedAt(LocalDateTime.now());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid QR code or user");
        }

        return checkAndCompleteSession(session);
    }

    @Transactional
    public ReturnDTOs.ReturnSessionResponse manualFallback(UUID listingId, User currentUser, ReturnDTOs.ManualFallbackRequest request) {
        if (!manualEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manual return method is disabled");
        }
        ReturnSession session = getActiveSession(listingId);

        String provided = request != null ? request.getItemNumber() : null;
        provided = provided != null ? provided.trim() : null;
        String expected = session.getListing() != null ? session.getListing().getItemReference() : null;
        expected = expected != null ? expected.trim() : null;
        if (expected != null && !expected.isEmpty()) {
            if (provided == null || !provided.equals(expected)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid item number");
            }
        } else {
            if (provided == null || provided.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing item number");
            }
        }

        boolean isBorrower = currentUser.getId().equals(session.getBorrower().getId());
        boolean isLender = currentUser.getId().equals(session.getLender().getId());
        boolean isPartnerAdmin = false;
        if (!isBorrower && !isLender) {
            Listing listing = session.getListing();
            if (listing != null && listing.getPartner() != null && listing.getPartner().getId() != null) {
                isPartnerAdmin = partnerAdminRepository.existsByUserAndPartnerAndRole(currentUser.getId(), listing.getPartner().getId(), PartnerAdminRole.ADMIN);
            }
        }

        if (isBorrower) {
            session.setManualBorrowerConfirmedAt(LocalDateTime.now());
        } else if (isLender || isPartnerAdmin) {
            session.setManualLenderConfirmedAt(LocalDateTime.now());
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not part of this return");
        }

        if (request.getConciergeWitnessId() != null && !request.getConciergeWitnessId().isEmpty()) {
            session.setConciergeWitnessId(request.getConciergeWitnessId());
        }

        return checkAndCompleteSession(session);
    }

    @Transactional
    public ReturnDTOs.ReturnSessionResponse denyManualReturn(UUID listingId, User currentUser, String reason) {
        ReturnSession session = getActiveSession(listingId);
        if (currentUser == null || currentUser.getId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not part of this return");
        }

        Listing listing = session.getListing();
        boolean isBorrower = session.getBorrower() != null && currentUser.getId().equals(session.getBorrower().getId());
        boolean isLender = session.getLender() != null && currentUser.getId().equals(session.getLender().getId());
        boolean isPartnerAdmin = false;
        if (!isBorrower && !isLender && listing != null && listing.getPartner() != null && listing.getPartner().getId() != null) {
            isPartnerAdmin = partnerAdminRepository.existsByUserAndPartnerAndRole(currentUser.getId(), listing.getPartner().getId(), PartnerAdminRole.ADMIN);
        }
        if (!isBorrower && !isLender && !isPartnerAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not part of this return");
        }

        if (session.getManualBorrowerConfirmedAt() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Borrower has not confirmed manual return");
        }

        session.setStatus(ReturnStatus.DISPUTED);
        session.setDisputeReason((reason != null && !reason.isBlank()) ? reason.trim() : "manual_return_denied");
        session.setExpiresAt(LocalDateTime.now());
        returnSessionRepository.save(session);

        if (listing != null) {
            listing.setStatus(AvailabilityStatus.DISPUTED);
            listingRepository.save(listing);
            escrowService.markDisputed(listingId, session.getDisputeReason());
        }

        return mapToResponse(session);
    }

    @Transactional
    public ReturnDTOs.ReturnSessionResponse initiateDispute(UUID listingId, User currentUser, ReturnDTOs.DisputeRequest request) {
        if (!disputeEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Dispute return method is disabled");
        }
        ReturnSession session = getActiveSession(listingId);
        if (currentUser == null || currentUser.getId() == null ||
                (!currentUser.getId().equals(session.getBorrower().getId()) && !currentUser.getId().equals(session.getLender().getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not part of this return");
        }

        session.setStatus(ReturnStatus.DISPUTED);
        session.setDisputeReason(request.getReason());
        session.setDisputePhotoUrl(request.getPhotoUrl());
        if (request.getConciergeWitnessId() != null) {
            session.setConciergeWitnessId(request.getConciergeWitnessId());
        }

        Listing listing = session.getListing();
        listing.setStatus(AvailabilityStatus.DISPUTED);
        listingRepository.save(listing);
        escrowService.markDisputed(listingId, request != null ? request.getReason() : "dispute");

        return mapToResponse(returnSessionRepository.save(session));
    }

    private ReturnSession getActiveSession(UUID listingId) {
        ReturnSession session = resolveSingleActiveSession(listingId);

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            // Auto dispute or expire
            session.setStatus(ReturnStatus.DISPUTED);
            session.getListing().setStatus(AvailabilityStatus.DISPUTED);
            listingRepository.save(session.getListing());
            returnSessionRepository.save(session);
            escrowService.markDisputed(listingId, "return_session_expired");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Return session expired and moved to dispute");
        }
        return session;
    }

    private ReturnDTOs.ReturnSessionResponse checkAndCompleteSession(ReturnSession session) {
        boolean bothScanned = session.getBorrowerScannedAt() != null && session.getLenderScannedAt() != null;
        boolean bothManual = session.getManualBorrowerConfirmedAt() != null && session.getManualLenderConfirmedAt() != null;

        if (bothScanned || (bothManual && session.getConciergeWitnessId() != null)) {
            session.setStatus(ReturnStatus.COMPLETED);
            Listing listing = session.getListing();
            listing.setStatus(listing.getPartner() != null ? AvailabilityStatus.PARTNER_ACTIVE : AvailabilityStatus.AVAILABLE);
            listing.setBorrower(null);
            listing.setItemReference(null);
            
            // Increase trust score (simple mock implementation for now)
            User lender = session.getLender();
            User borrower = session.getBorrower();
            lender.setTrustScore(Math.min(100, lender.getTrustScore() + 1));
            borrower.setTrustScore(Math.min(100, borrower.getTrustScore() + 1));
            
            userRepository.save(lender);
            userRepository.save(borrower);
            listingRepository.save(listing);
        }

        ReturnSession saved = returnSessionRepository.save(session);
        if (saved.getStatus() == ReturnStatus.COMPLETED) {
            escrowService.releaseOnSuccessfulReturn(saved);
            try {
                reviewInviteService.createAndSendForReturnSession(saved);
            } catch (Exception ignored) {
            }
        }
        return mapToResponse(saved);
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void checkExpiredSessions() {
        List<ReturnSession> expiredSessions = returnSessionRepository.findByStatusAndExpiresAtBefore(ReturnStatus.PENDING, LocalDateTime.now());
        for (ReturnSession session : expiredSessions) {
            session.setStatus(ReturnStatus.DISPUTED);
            session.setDisputeReason("Auto-dispute: 5-minute window expired without mutual scan.");
            Listing listing = session.getListing();
            listing.setStatus(AvailabilityStatus.DISPUTED);
            listingRepository.save(listing);
            returnSessionRepository.save(session);
            escrowService.markDisputed(listing.getId(), "auto_dispute_expired");
            // In a real system, notify support team here
        }
    }

    private ReturnDTOs.ReturnSessionResponse mapToResponse(ReturnSession session) {
        return ReturnDTOs.ReturnSessionResponse.builder()
                .id(session.getId())
                .listingId(session.getListing().getId())
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
    
    public ReturnDTOs.ReturnSessionResponse getSession(UUID listingId, User currentUser) {
        if (!anyReturnMethodEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Return methods are disabled");
        }
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));

        ReturnSession session = returnSessionRepository.findFirstByListingIdAndStatusOrderByCreatedAtDesc(listingId, ReturnStatus.PENDING)
                .orElse(null);

        if (session == null) {
            boolean listingActive = listing.getStatus() == AvailabilityStatus.BORROWED || listing.getStatus() == AvailabilityStatus.APPROVED || listing.getStatus() == AvailabilityStatus.PARTNER_ACTIVE || listing.getStatus() == AvailabilityStatus.DISPUTED;
            if (listingActive) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active return session");
            }
            session = returnSessionRepository.findFirstByListingIdOrderByCreatedAtDesc(listingId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No return session"));
        }
        if (currentUser == null || currentUser.getId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not part of this return");
        }
        UUID currentId = currentUser.getId();
        UUID borrowerId = session.getBorrower() != null ? session.getBorrower().getId() : null;
        UUID lenderId = session.getLender() != null ? session.getLender().getId() : null;
        if (!currentId.equals(borrowerId) && !currentId.equals(lenderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not part of this return");
        }
        return mapToResponse(session);
    }

    private ReturnSession getOrCreateActiveSession(Listing listing, User lender) {
        UUID listingId = listing.getId();
        List<ReturnSession> pending = returnSessionRepository.findByListingIdAndStatusOrderByCreatedAtDesc(listingId, ReturnStatus.PENDING);
        if (!pending.isEmpty()) {
            ReturnSession active = pending.get(0);
            for (int i = 1; i < pending.size(); i++) {
                ReturnSession extra = pending.get(i);
                extra.setStatus(ReturnStatus.DISPUTED);
                extra.setDisputeReason("Auto-dispute: duplicate active session cleanup.");
                returnSessionRepository.save(extra);
            }
            if (active.getExpiresAt() != null && active.getExpiresAt().isBefore(LocalDateTime.now())) {
                active.setStatus(ReturnStatus.DISPUTED);
                active.setDisputeReason("Auto-dispute: 5-minute window expired without mutual scan.");
                listing.setStatus(AvailabilityStatus.DISPUTED);
                listingRepository.save(listing);
                returnSessionRepository.save(active);
                escrowService.markDisputed(listingId, "return_session_expired");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Return session expired and moved to dispute");
            }
            return active;
        }

        String borrowerCode = generateSixDigitCode();
        String lenderCode = generateSixDigitCode();
        while (lenderCode.equals(borrowerCode)) {
            lenderCode = generateSixDigitCode();
        }
        return ReturnSession.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .borrower(listing.getBorrower())
                .lender(lender)
                .borrowerQrCode("BQR-" + UUID.randomUUID().toString())
                .lenderQrCode("LQR-" + UUID.randomUUID().toString())
                .borrowerCode(borrowerCode)
                .lenderCode(lenderCode)
                .status(ReturnStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
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

    private ReturnSession resolveSingleActiveSession(UUID listingId) {
        List<ReturnSession> pending = returnSessionRepository.findByListingIdAndStatusOrderByCreatedAtDesc(listingId, ReturnStatus.PENDING);
        if (pending.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active return session");
        }
        ReturnSession active = pending.get(0);
        for (int i = 1; i < pending.size(); i++) {
            ReturnSession extra = pending.get(i);
            extra.setStatus(ReturnStatus.DISPUTED);
            extra.setDisputeReason("Auto-dispute: duplicate active session cleanup.");
            returnSessionRepository.save(extra);
        }
        return active;
    }

    private void ensureCodes(ReturnSession session) {
        if (session.getBorrowerCode() != null && session.getLenderCode() != null) {
            return;
        }
        String borrowerCode = session.getBorrowerCode() != null ? session.getBorrowerCode() : generateSixDigitCode();
        String lenderCode = session.getLenderCode() != null ? session.getLenderCode() : generateSixDigitCode();
        while (lenderCode.equals(borrowerCode)) {
            lenderCode = generateSixDigitCode();
        }
        session.setBorrowerCode(borrowerCode);
        session.setLenderCode(lenderCode);
    }

    private boolean anyReturnMethodEnabled() {
        return qrEnabled() || manualEnabled() || disputeEnabled();
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
}
