package com.nearshare.api.repository;

import com.nearshare.api.model.Review;
import com.nearshare.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByTargetUser(User user);
}