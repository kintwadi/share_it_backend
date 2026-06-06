package com.vicinity24.api.service;

import com.vicinity24.api.model.EmailVerificationToken;
import com.vicinity24.api.model.User;
import com.vicinity24.api.repository.EmailVerificationTokenRepository;
import com.vicinity24.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class EmailVerificationService {
    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${email.verification.token.expiry.minutes:15}")
    private int tokenExpiryMinutes;

    public EmailVerificationService(UserRepository userRepository, EmailVerificationTokenRepository tokenRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    @Transactional
    public String startByEmail(String email, String language) {
        String normalized = email == null ? null : email.trim();
        if (normalized == null || normalized.isEmpty()) throw new IllegalArgumentException("invalid_email");
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalized);
        if (userOpt.isEmpty()) throw new IllegalArgumentException("user_not_found");
        User user = userOpt.get();
        if (Boolean.TRUE.equals(user.getEmailVerified())) return "";
        return startForUser(user, language);
    }

    @Transactional
    public String startForUser(User user, String language) {
        if (user == null || user.getId() == null) throw new IllegalArgumentException("user_not_found");
        if (Boolean.TRUE.equals(user.getEmailVerified())) return "";
        tokenRepository.invalidateUserTokens(user.getId());

        String code = generateRandomCode();
        String token = UUID.randomUUID().toString();
        EmailVerificationToken row = EmailVerificationToken.builder()
                .token(token)
                .code(code)
                .expiryDate(LocalDateTime.now().plusMinutes(tokenExpiryMinutes))
                .user(user)
                .used(false)
                .build();
        tokenRepository.save(row);
        emailService.sendSignupEmailVerificationEmail(user.getEmail(), user.getName(), code, language);
        return token;
    }

    @Transactional
    public User verify(String token, String code) {
        String t = token == null ? null : token.trim();
        String c = code == null ? null : code.trim();
        if (t == null || t.isEmpty()) throw new IllegalArgumentException("invalid_token");
        if (c == null || c.isEmpty()) throw new IllegalArgumentException("invalid_code");

        EmailVerificationToken row = tokenRepository.findByTokenAndCode(t, c).orElseThrow(() -> new IllegalArgumentException("invalid_code"));
        if (row.isExpired()) throw new IllegalArgumentException("code_expired");
        if (row.isUsed()) throw new IllegalArgumentException("code_used");

        User user = row.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        row.setUsed(true);
        tokenRepository.save(row);
        tokenRepository.invalidateUserTokens(user.getId());
        return user;
    }

    @Transactional
    public void resend(String token, String language) {
        String t = token == null ? null : token.trim();
        if (t == null || t.isEmpty()) throw new IllegalArgumentException("invalid_token");
        EmailVerificationToken row = tokenRepository.findByToken(t).orElseThrow(() -> new IllegalArgumentException("invalid_token"));
        if (row.isExpired()) throw new IllegalArgumentException("code_expired");
        if (row.isUsed()) throw new IllegalArgumentException("code_used");
        User user = row.getUser();
        emailService.sendSignupEmailVerificationEmail(user.getEmail(), user.getName(), row.getCode(), language);
    }

    private String generateRandomCode() {
        Random random = new Random();
        int code = 1000 + random.nextInt(9000);
        return String.valueOf(code);
    }
}
