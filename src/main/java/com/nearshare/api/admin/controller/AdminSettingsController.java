package com.nearshare.api.admin.controller;

import com.nearshare.api.config.RuntimeSettingsService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/app-settings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {
    private final RuntimeSettingsService runtimeSettingsService;

    public AdminSettingsController(RuntimeSettingsService runtimeSettingsService) {
        this.runtimeSettingsService = runtimeSettingsService;
    }

    @GetMapping
    public ResponseEntity<RuntimeSettingsService.AdminSettingsResponse> getEditableSettings() {
        return ResponseEntity.ok(runtimeSettingsService.getEditableSettings());
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateSettings(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody UpdateSettingsRequest body
    ) {
        String by = principal != null ? principal.getUsername() : "admin";
        List<RuntimeSettingsService.AdminSettingsUpdate> updates = body != null ? body.getUpdates() : null;
        runtimeSettingsService.applyUpdates(updates, by);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @Data
    public static class UpdateSettingsRequest {
        private List<RuntimeSettingsService.AdminSettingsUpdate> updates;
    }
}

