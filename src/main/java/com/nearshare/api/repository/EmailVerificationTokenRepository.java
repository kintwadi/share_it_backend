package com.nearshare.api.repository;

import com.nearshare.api.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByToken(String token);

    @Query("SELECT t FROM EmailVerificationToken t WHERE t.token = :token AND t.code = :code")
    Optional<EmailVerificationToken> findByTokenAndCode(@Param("token") String token, @Param("code") String code);

    @Query("SELECT t FROM EmailVerificationToken t WHERE LOWER(t.user.email) = LOWER(:email) AND t.used = false AND t.expiryDate > :now")
    Optional<EmailVerificationToken> findActiveByEmail(@Param("email") String email, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user.id = :userId")
    void invalidateUserTokens(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}

