package com.vicinity24.api.core.repository;

import com.vicinity24.api.core.model.SubscriptionVerificationCode;
import com.vicinity24.api.core.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionVerificationCodeRepository extends JpaRepository<SubscriptionVerificationCode, UUID> {

    @Query("SELECT c FROM SubscriptionVerificationCode c WHERE c.user = :user AND c.code = :code")
    Optional<SubscriptionVerificationCode> findByUserAndCode(@Param("user") User user, @Param("code") String code);

    @Query("SELECT c FROM SubscriptionVerificationCode c WHERE c.user = :user AND c.used = false AND c.expiryDate > :now")
    Optional<SubscriptionVerificationCode> findActiveCodeForUser(@Param("user") User user, @Param("now") LocalDateTime now);

    @Query("SELECT c FROM SubscriptionVerificationCode c WHERE c.user = :user AND c.code = :code ORDER BY c.expiryDate DESC")
    List<SubscriptionVerificationCode> findAllByUserAndCodeOrderByExpiryDateDesc(@Param("user") User user, @Param("code") String code);

    @Query("SELECT c FROM SubscriptionVerificationCode c WHERE c.user = :user AND c.used = false AND c.expiryDate > :now ORDER BY c.expiryDate DESC")
    List<SubscriptionVerificationCode> findActiveCodesForUser(@Param("user") User user, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM SubscriptionVerificationCode c WHERE c.user = :user")
    void deleteAllForUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM SubscriptionVerificationCode c WHERE c.expiryDate < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}
