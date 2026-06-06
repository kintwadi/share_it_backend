package com.vicinity24.api.controller;

import com.vicinity24.api.dto.SubscriptionDTO;
import com.vicinity24.api.dto.SubscriptionInvoiceDTO;
import com.vicinity24.api.dto.SubscriptionUpgradePreviewDTO;
import com.vicinity24.api.dto.SendSubscriptionCodeRequest;
import com.vicinity24.api.dto.VerifySubscriptionCodeRequest;
import com.vicinity24.api.config.RuntimeSettingsService;
import com.vicinity24.api.model.User;
import com.vicinity24.api.payment.StripePayment;
import com.vicinity24.api.service.SubscriptionService;
import com.vicinity24.api.service.UserService;
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
    private final RuntimeSettingsService runtimeSettingsService;

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

    public SubscriptionController(SubscriptionService subscriptionService, UserService userService, StripePayment stripePayment, RuntimeSettingsService runtimeSettingsService) {
        this.subscriptionService = subscriptionService;
        this.userService = userService;
        this.stripePayment = stripePayment;
        this.runtimeSettingsService = runtimeSettingsService;
    }

    private boolean isSubscriptionEnabled() {
        return runtimeSettingsService == null || runtimeSettingsService.isEnabled("settings.enable.subscription", true);
    }

    private void requireSubscriptionEnabled() {
        if (!isSubscriptionEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "subscription_disabled");
        }
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Boolean>> getSubscriptionConfig() {
        boolean starter = runtimeSettingsService.getBoolean("subscription.starter.enabled", starterEnabled);
        boolean plus = runtimeSettingsService.getBoolean("subscription.plus.enabled", plusEnabled);
        boolean pro = runtimeSettingsService.getBoolean("subscription.pro.enabled", proEnabled);
        return ResponseEntity.ok(Map.of(
            "enabled", isSubscriptionEnabled(),
            "starter", starter,
            "plus", plus,
            "pro", pro
        ));
    }

    @PostMapping("/send-code")
    public ResponseEntity<Map<String, Object>> sendVerificationCode(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody(required = false) SendSubscriptionCodeRequest request
    ) {
        requireSubscriptionEnabled();
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
        requireSubscriptionEnabled();
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        if (request == null || request.getCode() == null || request.getCode().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_verification_code"));
        }
        try {
            User user = userService.getByEmail(principal.getUsername());
            subscriptionService.verifyEmailCode(user, request.getCode());
            return ResponseEntity.ok(Map.of("status", "verified"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/starter")
    public ResponseEntity<Map<String, String>> subscribeStarter(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        requireSubscriptionEnabled();
        if (!runtimeSettingsService.getBoolean("subscription.starter.enabled", starterEnabled)) {
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
        requireSubscriptionEnabled();
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
            String effectivePlusPriceId = runtimeSettingsService.getString("subscription.plus.stripe_price_id", plusStripePriceId);
            String effectiveProPriceId = runtimeSettingsService.getString("subscription.pro.stripe_price_id", proStripePriceId);
            int effectivePlusTrialDays = runtimeSettingsService.getInt("subscription.plus.trial_days", plusTrialDays);
            int effectiveProTrialDays = runtimeSettingsService.getInt("subscription.pro.trial_days", proTrialDays);
            boolean effectivePlusEnabled = runtimeSettingsService.getBoolean("subscription.plus.enabled", plusEnabled);
            boolean effectiveProEnabled = runtimeSettingsService.getBoolean("subscription.pro.enabled", proEnabled);
            
            if ("pro".equalsIgnoreCase(planType)) {
                if (!effectiveProEnabled) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Pro plan is currently disabled"));
                }
                stripePriceId = ensureStripePriceConfigured("pro", effectiveProPriceId, principal.getUsername());
                trialDays = effectiveProTrialDays;
            } else {
                if (!effectivePlusEnabled) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Plus plan is currently disabled"));
                }
                stripePriceId = ensureStripePriceConfigured("plus", effectivePlusPriceId, principal.getUsername());
                trialDays = effectivePlusTrialDays;
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

    private synchronized String ensureStripePriceConfigured(String planType, String currentPriceId, String updatedBy) {
        String normalizedPlan = String.valueOf(planType == null ? "" : planType).trim().toLowerCase();
        if (!normalizedPlan.equals("plus") && !normalizedPlan.equals("pro")) {
            throw new IllegalArgumentException("Unsupported subscription plan: " + planType);
        }
        String priceSettingKey = normalizedPlan.equals("pro")
                ? "subscription.pro.stripe_price_id"
                : "subscription.plus.stripe_price_id";
        String configuredPriceId = runtimeSettingsService.getString(priceSettingKey, currentPriceId);
        String currency = runtimeSettingsService.getString("subscription.currency", "EUR");
        int amountCents = normalizedPlan.equals("pro")
                ? runtimeSettingsService.getInt("subscription.pro.monthly_amount_cents", 799)
                : runtimeSettingsService.getInt("subscription.plus.monthly_amount_cents", 499);
        String displayName = normalizedPlan.equals("pro") ? "Vicinity24 Pro" : "Vicinity24 Plus";

        com.vicinity24.api.payment.StripePayment.SubscriptionCatalogEntry entry =
                stripePayment.ensureSubscriptionCatalogEntry(
                        normalizedPlan,
                        displayName,
                        currency,
                        amountCents,
                        "month",
                        configuredPriceId
                );

        RuntimeSettingsService.AdminSettingsUpdate update = new RuntimeSettingsService.AdminSettingsUpdate();
        update.key = priceSettingKey;
        update.value = entry.priceId();
        runtimeSettingsService.applyUpdates(List.of(update), updatedBy != null ? updatedBy : "subscription-auto-provision");
        return entry.priceId();
    }

    @PostMapping("/sync-session")
    public ResponseEntity<Map<String, String>> syncFromSession(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody Map<String, String> body
    ) {
        requireSubscriptionEnabled();
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
            String effectivePlusPriceId = runtimeSettingsService.getString("subscription.plus.stripe_price_id", plusStripePriceId);
            String effectiveProPriceId = runtimeSettingsService.getString("subscription.pro.stripe_price_id", proStripePriceId);
            if (stripeSub.getItems() != null && stripeSub.getItems().getData() != null && !stripeSub.getItems().getData().isEmpty()) {
                 String priceId = stripeSub.getItems().getData().get(0).getPrice().getId();
                 if (priceId != null && priceId.equals(effectivePlusPriceId)) {
                     planType = "plus";
                 } else if (priceId != null && priceId.equals(effectiveProPriceId)) {
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
        requireSubscriptionEnabled();
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
        requireSubscriptionEnabled();
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
        if (!isSubscriptionEnabled()) {
            return ResponseEntity.noContent().build();
        }
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
        requireSubscriptionEnabled();
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
        requireSubscriptionEnabled();
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
        requireSubscriptionEnabled();
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        User user = userService.getByEmail(principal.getUsername());
        String newPlan = payload.get("newPlan");
        if (newPlan == null) return ResponseEntity.badRequest().build();
        
        return ResponseEntity.ok(subscriptionService.confirmUpgrade(user, newPlan));
    }
}
