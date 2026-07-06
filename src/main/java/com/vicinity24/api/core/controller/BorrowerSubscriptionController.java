package com.vicinity24.api.core.controller;

import com.vicinity24.api.core.dto.SubscriptionDTO;
import com.vicinity24.api.core.dto.SendSubscriptionCodeRequest;
import com.vicinity24.api.core.dto.VerifySubscriptionCodeRequest;
import com.vicinity24.api.core.model.User;
import com.vicinity24.api.core.service.BorrowerSubscriptionService;
import com.vicinity24.api.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/borrower-subscription")
public class BorrowerSubscriptionController {
    private static final Logger log = LoggerFactory.getLogger(BorrowerSubscriptionController.class);
    private final BorrowerSubscriptionService borrowerSubscriptionService;
    private final UserService userService;

    public BorrowerSubscriptionController(BorrowerSubscriptionService borrowerSubscriptionService, UserService userService) {
        this.borrowerSubscriptionService = borrowerSubscriptionService;
        this.userService = userService;
    }

    @PostMapping("/send-code")
    public ResponseEntity<Map<String, Object>> sendVerificationCode(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody(required = false) SendSubscriptionCodeRequest request
    ) {
        log.info("Borrower send-code called: principal={}, language={}",
                principal != null ? principal.getUsername() : "anonymous",
                request != null ? request.getLanguage() : null);
        User user = requireCurrentUser(principal);
        borrowerSubscriptionService.sendVerificationCode(user, request != null ? request.getLanguage() : null);
        log.info("Borrower send-code completed for user={}", user.getEmail());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<Map<String, String>> verifyCode(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody VerifySubscriptionCodeRequest request
    ) {
        User user = requireCurrentUser(principal);
        if (request == null || request.getCode() == null || request.getCode().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_verification_code"));
        }
        try {
            borrowerSubscriptionService.verifyCode(user, request.getCode());
            return ResponseEntity.ok(Map.of("status", "verified"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        User user = requireCurrentUser(principal);
        try {
            String returnPath = payload != null ? payload.get("returnPath") : null;
            return ResponseEntity.ok(borrowerSubscriptionService.createCheckoutSession(user, returnPath, principal.getUsername()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/sync-session")
    public ResponseEntity<Map<String, String>> syncFromSession(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody Map<String, String> body
    ) {
        User user = requireCurrentUser(principal);
        String sessionId = body != null ? body.get("sessionId") : null;
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionId is required"));
        }
        try {
            return ResponseEntity.ok(borrowerSubscriptionService.syncFromSession(user, sessionId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<SubscriptionDTO> getCurrentBorrowerSubscription(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal
    ) {
        User user = requireCurrentUser(principal);
        return ResponseEntity.ok(borrowerSubscriptionService.getCurrentBorrowerSubscription(user).orElse(null));
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, String>> unsubscribeBorrowerSubscription(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal
    ) {
        User user = requireCurrentUser(principal);
        try {
            borrowerSubscriptionService.cancelBorrowerSubscription(user);
            return ResponseEntity.ok(Map.of("status", "canceled"));
        } catch (RuntimeException e) {
            String message = e.getMessage() != null ? e.getMessage() : "borrower_subscription_unsubscribe_failed";
            if ("borrower_subscription_not_found".equals(message)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private User requireCurrentUser(org.springframework.security.core.userdetails.User principal) {
        if (principal == null) {
            log.warn("Borrower subscription request without authenticated principal");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        log.info("Resolving borrower current user for principal={}", principal.getUsername());
        return userService.getByEmail(principal.getUsername());
    }
}
