package com.vicinity24.api.core.repository;

import com.vicinity24.api.core.model.Listing;
import com.vicinity24.api.core.model.Review;
import com.vicinity24.api.core.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    @EntityGraph(attributePaths = {"author", "targetUser", "listing"})
    List<Review> findByTargetUser(User user);
    @EntityGraph(attributePaths = {"author", "targetUser", "listing"})
    List<Review> findByAuthor(User user);
    @EntityGraph(attributePaths = {"author", "targetUser", "listing"})
    List<Review> findByListing(Listing listing);
    boolean existsByAuthorAndTargetUserAndListing(User author, User targetUser, Listing listing);
}
