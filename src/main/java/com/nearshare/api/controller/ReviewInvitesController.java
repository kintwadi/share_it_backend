package com.nearshare.api.controller;

import com.nearshare.api.dto.ReviewDTO;
import com.nearshare.api.dto.ReviewInviteDTO;
import com.nearshare.api.model.ReviewInvite;
import com.nearshare.api.model.User;
import com.nearshare.api.repository.ReviewInviteRepository;
import com.nearshare.api.repository.ReviewRepository;
import com.nearshare.api.service.ReviewService;
import com.nearshare.api.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews/invite")
public class ReviewInvitesController {

    private final ReviewInviteRepository reviewInviteRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final UserService userService;

    public ReviewInvitesController(ReviewInviteRepository reviewInviteRepository, ReviewRepository reviewRepository, ReviewService reviewService, UserService userService) {
        this.reviewInviteRepository = reviewInviteRepository;
        this.reviewRepository = reviewRepository;
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<Object> getInvite(@PathVariable("token") String token) {
        ReviewInvite invite = reviewInviteRepository.findByToken(token).orElse(null);
        if (invite == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "invite_not_found"));
        }
        boolean expired = invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now());
        if (expired) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", "invite_expired"));
        }
        return ResponseEntity.ok(ReviewInviteDTO.builder()
                .token(invite.getToken())
                .listingId(invite.getListing() != null && invite.getListing().getId() != null ? invite.getListing().getId().toString() : null)
                .listingTitle(invite.getListing() != null ? invite.getListing().getTitle() : null)
                .reviewerId(invite.getReviewer() != null && invite.getReviewer().getId() != null ? invite.getReviewer().getId().toString() : null)
                .targetUserId(invite.getTargetUser() != null && invite.getTargetUser().getId() != null ? invite.getTargetUser().getId().toString() : null)
                .targetUserName(invite.getTargetUser() != null ? (invite.getTargetUser().getDisplayName() != null && !invite.getTargetUser().getDisplayName().isBlank() ? invite.getTargetUser().getDisplayName() : invite.getTargetUser().getName()) : null)
                .used(invite.getUsedAt() != null)
                .expiresAt(invite.getExpiresAt() != null ? invite.getExpiresAt().toString() : null)
                .build());
    }

    @PostMapping("/{token}")
    public ResponseEntity<Object> submitInviteReview(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @PathVariable("token") String token,
            @RequestBody Map<String, Object> payload
    ) {
        ReviewInvite invite = reviewInviteRepository.findByToken(token).orElse(null);
        if (invite == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "invite_not_found"));
        }
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", "invite_expired"));
        }
        if (invite.getUsedAt() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "invite_used"));
        }

        User author = invite.getReviewer();
        if (principal != null) {
            User current = userService.getByEmail(principal.getUsername());
            if (author == null || author.getId() == null || !author.getId().equals(current.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
            }
            author = current;
        }
        if (invite.getTargetUser() == null || invite.getListing() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "invite_invalid"));
        }
        if (reviewRepository.existsByAuthorAndTargetUserAndListing(author, invite.getTargetUser(), invite.getListing())) {
            invite.setUsedAt(LocalDateTime.now());
            reviewInviteRepository.save(invite);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "already_reviewed"));
        }

        int rating = payload.get("rating") instanceof Number n ? n.intValue() : 0;
        String comment = payload.get("comment") != null ? String.valueOf(payload.get("comment")) : "";
        if (rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_rating"));
        }
        var r = reviewService.create(invite.getTargetUser().getId(), invite.getListing().getId(), author, rating, comment);
        invite.setUsedAt(LocalDateTime.now());
        reviewInviteRepository.save(invite);
        return ResponseEntity.ok(ReviewDTO.builder()
                .id(r.getId())
                .authorId(r.getAuthor().getId())
                .targetUserId(r.getTargetUser().getId())
                .listingId(r.getListing().getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .timestamp(r.getTimestamp().toString())
                .build());
    }
}
