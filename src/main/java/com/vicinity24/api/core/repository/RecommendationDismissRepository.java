package com.vicinity24.api.core.repository;

import com.vicinity24.api.core.model.RecommendationDismiss;
import com.vicinity24.api.core.model.User;
import com.vicinity24.api.core.model.Listing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecommendationDismissRepository extends JpaRepository<RecommendationDismiss, UUID> {
    List<RecommendationDismiss> findByUser(User user);
    List<RecommendationDismiss> findByListing(Listing listing);
    boolean existsByUserAndListing(User user, Listing listing);
}
