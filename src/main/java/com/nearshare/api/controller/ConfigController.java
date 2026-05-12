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
    private final boolean allowAdminToggle;
    private final String stripePublicKey;
    private final SettingsProperties settingsProperties;

    public ConfigController(
            @Value("${nearshare.allowAdminToggle:false}") boolean allowAdminToggle,
            @Value("${STRIPE_PUBLIC_KEY}") String stripePublicKey,
            SettingsProperties settingsProperties) {
        this.allowAdminToggle = allowAdminToggle;
        this.stripePublicKey = stripePublicKey;
        this.settingsProperties = settingsProperties;
    }

    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> publicConfig() {
        return ResponseEntity.ok(Map.of(
            "allowAdminToggle", allowAdminToggle,
            "stripePublicKey", stripePublicKey,
            "connect", settingsProperties.getConnect(),
            "home", settingsProperties.getHome(),
            "borrowing", settingsProperties.getBorrowing()
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
