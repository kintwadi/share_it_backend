package com.nearshare.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private final boolean allowAdminToggle;
    private final String stripePublicKey;

    public ConfigController(
            @Value("${nearshare.allowAdminToggle:false}") boolean allowAdminToggle,
            @Value("${STRIPE_PUBLIC_KEY}") String stripePublicKey) {
        this.allowAdminToggle = allowAdminToggle;
        this.stripePublicKey = stripePublicKey;
    }

    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> publicConfig() {
        return ResponseEntity.ok(Map.of(
            "allowAdminToggle", allowAdminToggle,
            "stripePublicKey", stripePublicKey
        ));
    }
}