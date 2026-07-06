package com.vicinity24.api.core.controller;

import com.vicinity24.api.core.dto.ReviewDTO;
import com.vicinity24.api.core.model.User;
import com.vicinity24.api.core.service.ReviewService;
import com.vicinity24.api.core.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
public class ReviewsController {
    private final ReviewService reviewService;
    private final UserService userService;

    public ReviewsController(ReviewService reviewService, UserService userService) {
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewDTO>> getForUser(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(reviewService.getForUser(userId).stream().map(r -> ReviewDTO.builder()
                .id(r.getId())
                .authorId(r.getAuthor().getId())
                .reviewerName(resolveReviewerName(r.getAuthor()))
                .targetUserId(r.getTargetUser().getId())
                .listingId(r.getListing().getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .timestamp(r.getTimestamp().toString())
                .build()).toList());
    }

    @PostMapping("/")
    public ResponseEntity<ReviewDTO> create(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @RequestBody Map<String, Object> payload) {
        User author = userService.getByEmail(principal.getUsername());
        UUID targetUserId = UUID.fromString((String) payload.get("targetUserId"));
        UUID listingId = UUID.fromString((String) payload.get("listingId"));
        int rating = (int) payload.get("rating");
        String comment = (String) payload.get("comment");
        var r = reviewService.create(targetUserId, listingId, author, rating, comment);
        return ResponseEntity.ok(ReviewDTO.builder()
                .id(r.getId())
                .authorId(r.getAuthor().getId())
                .reviewerName(resolveReviewerName(r.getAuthor()))
                .targetUserId(r.getTargetUser().getId())
                .listingId(r.getListing().getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .timestamp(r.getTimestamp().toString())
                .build());
    }

    private String resolveReviewerName(User author) {
        if (author == null) return "Member";
        String displayName = author.getDisplayName();
        if (displayName != null && !displayName.isBlank()) return displayName;
        String name = author.getName();
        if (name != null && !name.isBlank()) return name;
        return "Member";
    }
}
