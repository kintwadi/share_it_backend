package com.vicinity24.api.core.admin.controller;

import com.vicinity24.api.core.config.RuntimeSettingsService;
import com.vicinity24.api.core.payment.StripePayment;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stripe")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStripeController {

    private final StripePayment stripePayment;
    private final RuntimeSettingsService runtimeSettingsService;

    @Value("${subscription.plus.stripe_price_id:}")
    private String plusStripePriceId;

    @Value("${subscription.pro.stripe_price_id:}")
    private String proStripePriceId;

    @Value("${subscription.plus.trial_days:14}")
    private int plusTrialDays;

    @Value("${subscription.pro.trial_days:14}")
    private int proTrialDays;

    public AdminStripeController(StripePayment stripePayment, RuntimeSettingsService runtimeSettingsService) {
        this.stripePayment = stripePayment;
        this.runtimeSettingsService = runtimeSettingsService;
    }

    @GetMapping("/diagnostics")
    public ResponseEntity<Map<String, Object>> diagnostics() {
        String effectivePlusPriceId = runtimeSettingsService.getString("subscription.plus.stripe_price_id", plusStripePriceId);
        String effectiveProPriceId = runtimeSettingsService.getString("subscription.pro.stripe_price_id", proStripePriceId);
        int effectivePlusTrialDays = runtimeSettingsService.getInt("subscription.plus.trial_days", plusTrialDays);
        int effectiveProTrialDays = runtimeSettingsService.getInt("subscription.pro.trial_days", proTrialDays);

        return ResponseEntity.ok(Map.of(
                "stripe", stripePayment.getDiagnostics(),
                "subscriptionConfig", Map.of(
                        "plusPriceId", valueOrFallback(effectivePlusPriceId),
                        "proPriceId", valueOrFallback(effectiveProPriceId),
                        "plusPriceConfigured", isConfigured(effectivePlusPriceId),
                        "proPriceConfigured", isConfigured(effectiveProPriceId),
                        "plusTrialDays", effectivePlusTrialDays,
                        "proTrialDays", effectiveProTrialDays
                )
        ));
    }

    @PostMapping("/provision-subscriptions")
    public ResponseEntity<Map<String, Object>> provisionSubscriptions(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody(required = false) ProvisionSubscriptionCatalogRequest body
    ) {
        String updatedBy = principal != null ? principal.getUsername() : "admin";
        String currency = normalizeCurrency(body != null ? body.getCurrency() : null);
        String currentPlusPriceId = runtimeSettingsService.getString("subscription.plus.stripe_price_id", plusStripePriceId);
        String currentProPriceId = runtimeSettingsService.getString("subscription.pro.stripe_price_id", proStripePriceId);
        int effectivePlusAmount = body != null && body.getPlusAmountCents() != null
                ? body.getPlusAmountCents()
                : runtimeSettingsService.getInt("subscription.plus.monthly_amount_cents", 499);
        int effectiveProAmount = body != null && body.getProAmountCents() != null
                ? body.getProAmountCents()
                : runtimeSettingsService.getInt("subscription.pro.monthly_amount_cents", 799);
        int effectivePlusTrialDays = body != null && body.getPlusTrialDays() != null
                ? body.getPlusTrialDays()
                : runtimeSettingsService.getInt("subscription.plus.trial_days", plusTrialDays);
        int effectiveProTrialDays = body != null && body.getProTrialDays() != null
                ? body.getProTrialDays()
                : runtimeSettingsService.getInt("subscription.pro.trial_days", proTrialDays);

        StripePayment.SubscriptionCatalogEntry plusEntry = stripePayment.ensureSubscriptionCatalogEntry(
                "plus",
                "Vicinity24 Plus",
                currency,
                effectivePlusAmount,
                "month",
                currentPlusPriceId
        );
        StripePayment.SubscriptionCatalogEntry proEntry = stripePayment.ensureSubscriptionCatalogEntry(
                "pro",
                "Vicinity24 Pro",
                currency,
                effectiveProAmount,
                "month",
                currentProPriceId
        );

        List<RuntimeSettingsService.AdminSettingsUpdate> updates = new ArrayList<>();
        updates.add(update("subscription.currency", currency.toUpperCase(Locale.ROOT)));
        updates.add(update("subscription.plus.monthly_amount_cents", effectivePlusAmount));
        updates.add(update("subscription.pro.monthly_amount_cents", effectiveProAmount));
        updates.add(update("subscription.plus.trial_days", effectivePlusTrialDays));
        updates.add(update("subscription.pro.trial_days", effectiveProTrialDays));
        updates.add(update("subscription.plus.stripe_price_id", plusEntry.priceId()));
        updates.add(update("subscription.pro.stripe_price_id", proEntry.priceId()));
        runtimeSettingsService.applyUpdates(updates, updatedBy);

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Stripe subscription catalog provisioned and stored in runtime settings",
                "stripe", stripePayment.getDiagnostics(),
                "subscriptionConfig", Map.of(
                        "currency", currency.toUpperCase(Locale.ROOT),
                        "plusTrialDays", effectivePlusTrialDays,
                        "proTrialDays", effectiveProTrialDays,
                        "plus", Map.of(
                                "productId", plusEntry.productId(),
                                "priceId", plusEntry.priceId(),
                                "unitAmountCents", plusEntry.unitAmountCents(),
                                "reusedExistingPrice", plusEntry.reusedExistingPrice()
                        ),
                        "pro", Map.of(
                                "productId", proEntry.productId(),
                                "priceId", proEntry.priceId(),
                                "unitAmountCents", proEntry.unitAmountCents(),
                                "reusedExistingPrice", proEntry.reusedExistingPrice()
                        )
                )
        ));
    }

    private boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }

    private String valueOrFallback(String value) {
        return isConfigured(value) ? value : "not_configured";
    }

    private String normalizeCurrency(String value) {
        String configured = runtimeSettingsService.getString("subscription.currency", "EUR");
        String chosen = value != null && !value.isBlank() ? value : configured;
        return chosen.trim().toLowerCase(Locale.ROOT);
    }

    private RuntimeSettingsService.AdminSettingsUpdate update(String key, Object value) {
        RuntimeSettingsService.AdminSettingsUpdate update = new RuntimeSettingsService.AdminSettingsUpdate();
        update.key = key;
        update.value = value;
        return update;
    }

    @Data
    public static class ProvisionSubscriptionCatalogRequest {
        private String currency;
        private Integer plusAmountCents;
        private Integer proAmountCents;
        private Integer plusTrialDays;
        private Integer proTrialDays;
    }
}
