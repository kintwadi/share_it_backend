package com.nearshare.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nearshare.api.config.RuntimeSettingsService;
import com.nearshare.api.config.SettingsProperties;
import jakarta.annotation.PostConstruct;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private final String stripePublicKey;
    private final SettingsProperties settingsProperties;
    private final RuntimeSettingsService runtimeSettingsService;
    private final int subscriptionPlusTrialDays;
    private final int subscriptionPlusMonthlyAmountCents;
    private final String subscriptionCurrency;

    public ConfigController(
            @Value("${STRIPE_PUBLIC_KEY}") String stripePublicKey,
            @Value("${subscription.plus.trial_days:14}") int subscriptionPlusTrialDays,
            @Value("${subscription.plus.monthly_amount_cents:499}") int subscriptionPlusMonthlyAmountCents,
            @Value("${subscription.currency:EUR}") String subscriptionCurrency,
            SettingsProperties settingsProperties,
            RuntimeSettingsService runtimeSettingsService) {
        this.stripePublicKey = stripePublicKey;
        this.subscriptionPlusTrialDays = subscriptionPlusTrialDays;
        this.subscriptionPlusMonthlyAmountCents = subscriptionPlusMonthlyAmountCents;
        this.subscriptionCurrency = subscriptionCurrency;
        this.settingsProperties = settingsProperties;
        this.runtimeSettingsService = runtimeSettingsService;
    }

    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> publicConfig() {
        Map<String, Object> effective = runtimeSettingsService.getEffectiveSettings();
        int plusTrialDays = runtimeSettingsService.getInt("subscription.plus.trial_days", subscriptionPlusTrialDays);
        int plusMonthlyAmountCents = runtimeSettingsService.getInt("subscription.plus.monthly_amount_cents", subscriptionPlusMonthlyAmountCents);
        String currency = runtimeSettingsService.getString("subscription.currency", subscriptionCurrency);
        return ResponseEntity.ok(Map.of(
            "stripePublicKey", stripePublicKey,
            "connect", effective.getOrDefault("connect", settingsProperties.getConnect()),
            "home", effective.getOrDefault("home", settingsProperties.getHome()),
            "borrowing", effective.getOrDefault("borrowing", settingsProperties.getBorrowing()),
            "subscription", Map.of(
                "plusTrialDays", plusTrialDays,
                "plusMonthlyAmountCents", plusMonthlyAmountCents,
                "currency", currency
            )
        ));
    }

    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> settingsConfig() {
        return ResponseEntity.ok(runtimeSettingsService.getEffectiveSettings());
    }

    @PostConstruct
    public void logSettings() {
        System.out.println("Loaded Settings Configuration: " + settingsProperties);
    }
}
