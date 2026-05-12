package com.nearshare.api.repository;

import com.nearshare.api.model.RecommendationDismiss;
import com.nearshare.api.model.User;
import com.nearshare.api.model.Listing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecommendationDismissRepository extends JpaRepository<RecommendationDismiss, UUID> {
    List<RecommendationDismiss> findByUser(User user);
    List<RecommendationDismiss> findByListing(Listing listing);
    boolean existsByUserAndListing(User user, Listing listing);
}
