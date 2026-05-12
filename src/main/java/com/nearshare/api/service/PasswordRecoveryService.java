package com.nearshare.api.service;

import com.nearshare.api.dto.ForgotPasswordRequest;
import com.nearshare.api.dto.ResetPasswordRequest;
import com.nearshare.api.dto.VerifyResetCodeRequest;
import com.nearshare.api.model.PasswordResetToken;
import com.nearshare.api.model.User;
import com.nearshare.api.repository.PasswordResetTokenRepository;
import com.nearshare.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class PasswordRecoveryService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    @Value("${password.reset.token.expiry.minutes:15}")
    private int tokenExpiryMinutes;
    
    public PasswordRecoveryService(UserRepository userRepository, 
                                  PasswordResetTokenRepository tokenRepository,
                                  PasswordEncoder passwordEncoder,
                                  EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }
    
    @Transactional
    public void initiatePasswordReset(ForgotPasswordRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim();
        Optional<User> userOpt = email == null ? Optional.empty() : userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            // Don't reveal if user exists or not for security
            return;
        }
        
        User user = userOpt.get();
        
        // Invalidate any existing tokens for this user
        tokenRepository.invalidateUserTokens(user.getId());
        
        // Generate 4-digit code
        String code = generateRandomCode();
        String token = UUID.randomUUID().toString();
        
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .code(code)
                .expiryDate(LocalDateTime.now().plusMinutes(tokenExpiryMinutes))
                .user(user)
                .used(false)
                .build();
        
        tokenRepository.save(resetToken);
        
        // Send email with code
        emailService.sendPasswordResetEmail(user.getEmail(), code);
    }
    
    public String verifyResetCode(VerifyResetCodeRequest request) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PasswordRecoveryService.class);
        logger.info("Verifying reset code for email: '{}', code: '{}'", request.getEmail(), request.getCode());

        String email = request.getEmail() == null ? null : request.getEmail().trim();
        String code = request.getCode() == null ? null : request.getCode().trim();
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByCodeAndUserEmail(
            code, email);
        
        if (tokenOpt.isEmpty()) {
            logger.warn("No token found for email: '{}' and code: '{}'", email, code);
            // Debug: check if any token exists for this email regardless of code
            tokenRepository.findActiveTokenByUserEmail(email, LocalDateTime.now())
                .ifPresentOrElse(
                    t -> logger.warn("Found active token for email '{}' but code mismatch. Expected: '{}', Actual: '{}'", 
                        email, t.getCode(), code),
                    () -> logger.warn("No active token found at all for email '{}'", email)
                );
            throw new RuntimeException("Invalid verification code");
        }
        
        PasswordResetToken token = tokenOpt.get();
        
        if (token.isExpired()) {
            throw new RuntimeException("Verification code has expired");
        }
        
        if (token.isUsed()) {
            throw new RuntimeException("Verification code already used");
        }
        
        return token.getToken();
    }
    
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(request.getToken());
        
        if (tokenOpt.isEmpty()) {
            throw new RuntimeException("Invalid reset token");
        }
        
        PasswordResetToken token = tokenOpt.get();
        
        if (token.isExpired()) {
            throw new RuntimeException("Reset token has expired");
        }
        
        if (token.isUsed()) {
            throw new RuntimeException("Reset token already used");
        }
        
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // Mark token as used
        token.setUsed(true);
        tokenRepository.save(token);
        
        // Invalidate all other tokens for this user
        tokenRepository.invalidateUserTokens(user.getId());
    }
    
    private String generateRandomCode() {
        Random random = new Random();
        int code = 1000 + random.nextInt(9000); // 4-digit code between 1000-9999
        return String.valueOf(code);
    }
}
