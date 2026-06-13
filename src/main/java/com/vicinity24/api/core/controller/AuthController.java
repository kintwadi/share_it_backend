package com.vicinity24.api.core.controller;

import com.vicinity24.api.core.model.User;
import com.vicinity24.api.core.dto.ForgotPasswordRequest;
import com.vicinity24.api.core.dto.LoginRequest;
import com.vicinity24.api.core.dto.RegisterRequest;
import com.vicinity24.api.core.dto.RegisterResponse;
import com.vicinity24.api.core.dto.ResendEmailVerificationRequest;
import com.vicinity24.api.core.dto.ResetPasswordRequest;
import com.vicinity24.api.core.dto.StartEmailVerificationRequest;
import com.vicinity24.api.core.dto.TokenResponse;
import com.vicinity24.api.core.dto.VerifyEmailVerificationRequest;
import com.vicinity24.api.core.dto.VerifyResetCodeRequest;
import com.vicinity24.api.core.config.RuntimeSettingsService;
import com.vicinity24.api.core.service.AuthService;
import com.vicinity24.api.core.service.EmailVerificationService;
import com.vicinity24.api.core.service.PasswordRecoveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final PasswordRecoveryService passwordRecoveryService;
    private final EmailVerificationService emailVerificationService;
    private final RuntimeSettingsService runtimeSettingsService;

    @Value("${setting.signup.email.verification.required:true}")
    private boolean signupEmailVerificationRequired;

    public AuthController(AuthService authService, PasswordRecoveryService passwordRecoveryService, EmailVerificationService emailVerificationService, RuntimeSettingsService runtimeSettingsService) {
        this.authService = authService;
        this.passwordRecoveryService = passwordRecoveryService;
        this.emailVerificationService = emailVerificationService;
        this.runtimeSettingsService = runtimeSettingsService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest.getHeader("User-Agent"), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/verify-2fa-login")
    public ResponseEntity<TokenResponse> verify2faLogin(@RequestBody java.util.Map<String, String> body, @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, jakarta.servlet.http.HttpServletRequest httpRequest) {
        String code = body.get("code");
        return ResponseEntity.ok(authService.verify2faLogin(principal.getUsername(), code, httpRequest.getHeader("User-Agent"), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/email-verification/start")
    public ResponseEntity<java.util.Map<String, Object>> startEmailVerification(@RequestBody StartEmailVerificationRequest request) {
        if (!signupEmailVerificationRequired) {
            throw new IllegalArgumentException("email_verification_not_required");
        }
        String token = emailVerificationService.startByEmail(request != null ? request.getEmail() : null, request != null ? request.getLanguage() : null);
        return ResponseEntity.ok(java.util.Map.of("status", "ok", "token", token));
    }

    @PostMapping("/email-verification/verify")
    public ResponseEntity<TokenResponse> verifyEmailVerification(@RequestBody VerifyEmailVerificationRequest request) {
        if (!signupEmailVerificationRequired) {
            throw new IllegalArgumentException("email_verification_not_required");
        }
        User user = emailVerificationService.verify(request != null ? request.getToken() : null, request != null ? request.getCode() : null);
        return ResponseEntity.ok(authService.tokenForUser(user));
    }

    @PostMapping("/email-verification/resend")
    public ResponseEntity<java.util.Map<String, Object>> resendEmailVerification(@RequestBody ResendEmailVerificationRequest request) {
        if (!signupEmailVerificationRequired) {
            throw new IllegalArgumentException("email_verification_not_required");
        }
        emailVerificationService.resend(request != null ? request.getToken() : null, request != null ? request.getLanguage() : null);
        return ResponseEntity.ok(java.util.Map.of("status", "ok"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordRecoveryService.initiatePasswordReset(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verifyResetCode(@RequestBody VerifyResetCodeRequest request) {
        String token = passwordRecoveryService.verifyResetCode(request);
        return ResponseEntity.ok(java.util.Map.of("valid", true, "token", token));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordRecoveryService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}
