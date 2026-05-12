package com.nearshare.api.controller;

import com.nearshare.api.dto.SubscriptionDTO;
import com.nearshare.api.dto.SubscriptionInvoiceDTO;
import com.nearshare.api.dto.SubscriptionUpgradePreviewDTO;
import com.nearshare.api.dto.SendSubscriptionCodeRequest;
import com.nearshare.api.dto.VerifySubscriptionCodeRequest;
import com.nearshare.api.model.User;
import com.nearshare.api.payment.StripePayment;
import com.nearshare.api.service.SubscriptionService;
import com.nearshare.api.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.model.checkout.Session;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;
    private final StripePayment stripePayment;

    @Value("${subscription.plus.stripe_price_id:}")
    private String plusStripePriceId;

    @Value("${subscription.plus.trial_days:14}")
    private int plusTrialDays;

    @Value("${subscription.pro.stripe_price_id:}")
    private String proStripePriceId;

    @Value("${subscription.pro.trial_days:14}")
    private int proTrialDays;

    @Value("${FRONTEND_BASE_URL:http://localhost:3001}")
    private String frontendBaseUrl;

    @Value("${subscription.starter.enabled:true}")
    private boolean starterEnabled;

    @Value("${subscription.plus.enabled:true}")
    private boolean plusEnabled;

    @Value("${subscription.pro.enabled:true}")
    private boolean proEnabled;

    public SubscriptionController(SubscriptionService subscriptionService, UserService userService, StripePayment stripePayment) {
        this.subscriptionService = subscriptionService;
        this.userService = userService;
        this.stripePayment = stripePayment;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Boolean>> getSubscriptionConfig() {
        return ResponseEntity.ok(Map.of(
            "starter", starterEnabled,
            "plus", plusEnabled,
            "pro", proEnabled
        ));
    }

    @PostMapping("/send-code")
    public ResponseEntity<Map<String, Object>> sendVerificationCode(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody(required = false) SendSubscriptionCodeRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        User user = userService.getByEmail(principal.getUsername());
        subscriptionService.sendVerificationCode(
                user,
                request != null ? request.getPlanType() : null,
                request != null ? request.getLanguage() : null
        );
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<Map<String, String>> verifyCode(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
                                                             @RequestBody VerifySubscriptionCodeRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        User user = userService.getByEmail(principal.getUsername());
        subscriptionService.verifyEmailCode(user, request.getCode());
        return ResponseEntity.ok(Map.of("status", "verified"));
    }

    @PostMapping("/starter")
    public ResponseEntity<Map<String, String>> subscribeStarter(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        if (!starterEnabled) {
            return ResponseEntity.badRequest().body(Map.of("error", "Starter plan is currently disabled"));
        }
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        User user = userService.getByEmail(principal.getUsername());
        subscriptionService.createStarterSubscription(user);
        return ResponseEntity.ok(Map.of("status", "active", "plan", "starter"));
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
            }
            User user = userService.getByEmail(principal.getUsername());
            
            // Check if user already has an active subscription
            if (subscriptionService.hasActivePaidSubscription(user)) {
                return ResponseEntity.badRequest().body(Map.of("error", "User already has an active paid subscription"));
            }
            
            String planType = (payload != null && payload.get("planType") != null) ? payload.get("planType") : "plus";
            String stripePriceId;
            int trialDays = 0;
            
            if ("pro".equalsIgnoreCase(planType)) {
                if (!proEnabled) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Pro plan is currently disabled"));
                }
                stripePriceId = proStripePriceId;
                trialDays = proTrialDays;
            } else {
                if (!plusEnabled) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Plus plan is currently disabled"));
                }
                stripePriceId = plusStripePriceId;
                trialDays = plusTrialDays;
            }
            
            if (stripePriceId == null || stripePriceId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Stripe price is not configured for plan: " + planType));
            }

            String returnPath = (payload != null && payload.get("returnPath") != null) ? payload.get("returnPath") : "/dashboard";
            // Ensure returnPath starts with / if not empty
            if (!returnPath.startsWith("/")) returnPath = "/" + returnPath;
            
            String successUrl = frontendBaseUrl + returnPath;
            String cancelUrl = frontendBaseUrl + "/subscription/checkout";
            Session session = stripePayment.createSubscriptionCheckoutSession(
                    stripePriceId,
                    trialDays,
                    successUrl,
                    cancelUrl,
                    user.getEmail(),
                    user.getId().toString()
            );
            return ResponseEntity.ok(Map.of(
                    "sessionId", session.getId(),
                    "url", session.getUrl()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

    }

    @PostMapping("/sync-session")
    public ResponseEntity<Map<String, String>> syncFromSession(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody Map<String, String> body
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        }
        String sessionId = body.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionId is required"));
        }
        try {
            User user = userService.getByEmail(principal.getUsername());
            Session session = stripePayment.retrieveCheckoutSession(sessionId);
            String subscriptionId = session.getSubscription() != null ? session.getSubscription().toString() : null;
            if (subscriptionId == null || subscriptionId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No subscription associated with this session"));
            }
            com.stripe.model.Subscription stripeSub = com.stripe.model.Subscription.retrieve(subscriptionId);
            String status = stripeSub.getStatus();
            
            String planType = null;
            if (stripeSub.getItems() != null && stripeSub.getItems().getData() != null && !stripeSub.getItems().getData().isEmpty()) {
                 String priceId = stripeSub.getItems().getData().get(0).getPrice().getId();
                 if (priceId.equals(plusStripePriceId)) {
                     planType = "plus";
                 } else if (priceId.equals(proStripePriceId)) {
                     planType = "pro";
                 }
            }
            
            subscriptionService.syncProSubscriptionFromStripe(user, subscriptionId, status, planType);
            return ResponseEntity.ok(Map.of("status", "synced", "stripeSubscriptionId", subscriptionId, "stripeStatus", status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, String>> cancelSubscription(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        }
        User user = userService.getByEmail(principal.getUsername());
        subscriptionService.cancelProSubscription(user);
        return ResponseEntity.ok(Map.of("status", "canceled"));
    }

    // Temporary endpoint to fix subscription status mismatch
    @PostMapping("/admin/fix-status")
    public ResponseEntity<Map<String, String>> fixSubscriptionStatus(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        }
        User user = userService.getByEmail(principal.getUsername());
        
        // Get the current subscription and update its status to "canceled"
        var result = subscriptionService.fixSubscriptionStatus(user);
        
        return ResponseEntity.ok(Map.of("status", "fixed", "message", result));
    }

    @GetMapping("/me")
    public ResponseEntity<SubscriptionDTO> getCurrentSubscription(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        User user = userService.getByEmail(principal.getUsername());
        return subscriptionService.getCurrentSubscription(user)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<SubscriptionInvoiceDTO>> getSubscriptionInvoices(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        User user = userService.getByEmail(principal.getUsername());
        List<SubscriptionInvoiceDTO> invoices = subscriptionService.getInvoicesForUser(user);
        return ResponseEntity.ok(invoices);
    }

    @PostMapping("/upgrade/preview")
    public ResponseEntity<SubscriptionUpgradePreviewDTO> previewUpgrade(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody Map<String, String> payload) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        User user = userService.getByEmail(principal.getUsername());
        String newPlan = payload.get("newPlan");
        if (newPlan == null) return ResponseEntity.badRequest().build();
        
        return ResponseEntity.ok(subscriptionService.previewUpgrade(user, newPlan));
    }

    @PostMapping("/upgrade/confirm")
    public ResponseEntity<SubscriptionDTO> confirmUpgrade(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody Map<String, String> payload) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        User user = userService.getByEmail(principal.getUsername());
        String newPlan = payload.get("newPlan");
        if (newPlan == null) return ResponseEntity.badRequest().build();
        
        return ResponseEntity.ok(subscriptionService.confirmUpgrade(user, newPlan));
    }
}
