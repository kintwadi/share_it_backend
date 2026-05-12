package com.nearshare.api.repository;

import com.nearshare.api.model.Subscription;
import com.nearshare.api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findFirstByUserOrderByCreatedAtDesc(User user);
    Optional<Subscription> findFirstByUserAndStripeSubscriptionIdIsNotNullOrderByCreatedAtDesc(User user);
    List<Subscription> findByUser(User user);
    List<Subscription> findByUserAndStatusIn(User user, List<String> statuses);
    Optional<Subscription> findFirstByStripeSubscriptionIdOrderByCreatedAtDesc(String stripeSubscriptionId);
    Page<Subscription> findByStatus(String status, Pageable pageable);
}
