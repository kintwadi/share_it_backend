package com.vicinity24.api.core.service;

import com.vicinity24.api.core.model.Listing;
import com.vicinity24.api.core.model.Review;
import com.vicinity24.api.core.model.User;
import com.vicinity24.api.core.repository.ListingRepository;
import com.vicinity24.api.core.repository.ReviewRepository;
import com.vicinity24.api.core.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository, ListingRepository listingRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
    }

    @Transactional(readOnly = true)
    public List<Review> getForUser(UUID userId) {
        User u = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("user_not_found"));
        return reviewRepository.findByTargetUser(u);
    }

    @Transactional
    public Review create(UUID targetUserId, UUID listingId, User author, int rating, String comment) {
        User target = userRepository.findById(targetUserId).orElseThrow(() -> new RuntimeException("user_not_found"));
        Listing listing = listingRepository.findById(listingId).orElseThrow(() -> new RuntimeException("listing_not_found"));
        Review r = Review.builder().id(UUID.randomUUID()).rating(rating).comment(comment).timestamp(LocalDateTime.now()).author(author).targetUser(target).listing(listing).build();
        reviewRepository.save(r);
        List<Review> reviews = reviewRepository.findByTargetUser(target);
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
        target.setTrustScore((int) Math.round(avg * 20));
        userRepository.save(target);
        return r;
    }
}
