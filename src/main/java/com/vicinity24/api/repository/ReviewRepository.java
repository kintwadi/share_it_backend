package com.vicinity24.api.repository;

import com.vicinity24.api.model.Listing;
import com.vicinity24.api.model.Review;
import com.vicinity24.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByTargetUser(User user);
    List<Review> findByAuthor(User user);
    List<Review> findByListing(Listing listing);
    boolean existsByAuthorAndTargetUserAndListing(User author, User targetUser, Listing listing);
}
