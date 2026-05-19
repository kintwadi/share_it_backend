package com.nearshare.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nearshare.api.config.SettingsProperties;
import jakarta.annotation.PostConstruct;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private final String stripePublicKey;
    private final SettingsProperties settingsProperties;
    private final int subscriptionPlusTrialDays;
    private final int subscriptionPlusMonthlyAmountCents;
    private final String subscriptionCurrency;

    public ConfigController(
            @Value("${STRIPE_PUBLIC_KEY}") String stripePublicKey,
            @Value("${subscription.plus.trial_days:14}") int subscriptionPlusTrialDays,
            @Value("${subscription.plus.monthly_amount_cents:499}") int subscriptionPlusMonthlyAmountCents,
            @Value("${subscription.currency:EUR}") String subscriptionCurrency,
            SettingsProperties settingsProperties) {
        this.stripePublicKey = stripePublicKey;
        this.subscriptionPlusTrialDays = subscriptionPlusTrialDays;
        this.subscriptionPlusMonthlyAmountCents = subscriptionPlusMonthlyAmountCents;
        this.subscriptionCurrency = subscriptionCurrency;
        this.settingsProperties = settingsProperties;
    }

    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> publicConfig() {
        return ResponseEntity.ok(Map.of(
            "stripePublicKey", stripePublicKey,
            "connect", settingsProperties.getConnect(),
            "home", settingsProperties.getHome(),
            "borrowing", settingsProperties.getBorrowing(),
            "subscription", Map.of(
                "plusTrialDays", subscriptionPlusTrialDays,
                "plusMonthlyAmountCents", subscriptionPlusMonthlyAmountCents,
                "currency", subscriptionCurrency
            )
        ));
    }

    @GetMapping("/settings")
    public ResponseEntity<SettingsProperties> settingsConfig() {
        return ResponseEntity.ok(settingsProperties);
    }

    @PostConstruct
    public void logSettings() {
        System.out.println("Loaded Settings Configuration: " + settingsProperties);
    }
}
