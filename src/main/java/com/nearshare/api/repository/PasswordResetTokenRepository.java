package com.nearshare.api.repository;

import com.nearshare.api.model.PasswordResetToken;
import com.nearshare.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    @Query("SELECT t FROM PasswordResetToken t WHERE t.code = :code AND LOWER(t.user.email) = LOWER(:email)")
    Optional<PasswordResetToken> findByCodeAndUserEmail(@Param("code") String code, @Param("email") String email);
    
    @Query("SELECT t FROM PasswordResetToken t WHERE t.user.email = :email AND t.used = false AND t.expiryDate > :now")
    Optional<PasswordResetToken> findActiveTokenByUserEmail(@Param("email") String email, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
    void invalidateUserTokens(@Param("userId") UUID userId);
}
