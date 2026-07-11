package com.vicinity24.api.core.service;

import com.vicinity24.api.core.model.Listing;
import com.vicinity24.api.core.model.ReturnSession;
import com.vicinity24.api.core.model.ReviewInvite;
import com.vicinity24.api.core.model.User;
import com.vicinity24.api.core.model.enums.ReturnStatus;
import com.vicinity24.api.core.repository.ReviewInviteRepository;
import com.vicinity24.api.core.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewInviteService {

    private final ReviewInviteRepository reviewInviteRepository;
    private final ReviewRepository reviewRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.frontend.baseUrl:http://localhost:3001}")
    private String frontendBaseUrl;

    public void createAndSendForReturnSession(ReturnSession session) {
        if (session == null || session.getId() == null) {
            return;
        }
        if (session.getStatus() != ReturnStatus.COMPLETED) {
            return;
        }

        Listing listing = session.getListing();
        User lender = session.getLender();
        User borrower = session.getBorrower();
        if (listing == null || listing.getId() == null || lender == null || borrower == null) {
            return;
        }

        createAndSendSingle(session.getId(), listing, lender, borrower);
        createAndSendSingle(session.getId(), listing, borrower, lender);
    }

    private void createAndSendSingle(UUID returnSessionId, Listing listing, User reviewer, User target) {
        if (reviewer == null || reviewer.getId() == null || reviewer.getEmail() == null || reviewer.getEmail().isBlank()) {
            return;
        }
        if (target == null || target.getId() == null) {
            return;
        }

        Optional<ReviewInvite> existing = reviewInviteRepository.findFirstByReturnSessionIdAndReviewerIdAndTargetUserId(returnSessionId, reviewer.getId(), target.getId());
        ReviewInvite invite = existing.orElseGet(() -> ReviewInvite.builder()
                .id(UUID.randomUUID())
                .token(generateToken())
                .returnSessionId(returnSessionId)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .usedAt(null)
                .reviewer(reviewer)
                .targetUser(target)
                .listing(listing)
                .build());

        if (invite.getId() == null) {
            invite.setId(UUID.randomUUID());
        }
        if (invite.getToken() == null || invite.getToken().isBlank()) {
            invite.setToken(generateToken());
        }
        if (invite.getCreatedAt() == null) {
            invite.setCreatedAt(LocalDateTime.now());
        }
        if (invite.getExpiresAt() == null) {
            invite.setExpiresAt(LocalDateTime.now().plusDays(30));
        }

        ReviewInvite saved = reviewInviteRepository.save(invite);
        if (saved.getUsedAt() != null) {
            return;
        }

        String link = buildFrontendAppUrl("/rate?token=" + saved.getToken());
        String listingTitle = listing.getTitle();
        String listingReference = listing.getItemReference();
        String recipientName = reviewer.getDisplayName() != null && !reviewer.getDisplayName().isBlank() ? reviewer.getDisplayName() : reviewer.getName();
        String otherName = target.getDisplayName() != null && !target.getDisplayName().isBlank() ? target.getDisplayName() : target.getName();

        emailService.sendReturnRatingEmail(reviewer.getEmail(), recipientName, otherName, listingTitle, listingReference, link);
    }

    private String generateToken() {
        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String buildFrontendAppUrl(String routePath) {
        String base = frontendBaseUrl == null ? "" : frontendBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String path = routePath == null || routePath.isBlank() ? "/" : routePath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (base.contains("/#/")) {
            return base + path;
        }
        if (base.endsWith("/#")) {
            return base + path;
        }
        return base + "/#" + path;
    }
}
