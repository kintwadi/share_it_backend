package com.vicinity24.api.controller;

import com.vicinity24.api.dto.ChangePasswordRequest;
import com.vicinity24.api.dto.UpdateProfileRequest;
import com.vicinity24.api.dto.UserDTO;
import com.vicinity24.api.dto.UserSummaryDTO;
import com.vicinity24.api.config.RuntimeSettingsService;
import com.vicinity24.api.model.User;
import com.vicinity24.api.model.enums.UserStatus;
import com.vicinity24.api.service.UserService;
import com.vicinity24.api.storage.StorageManager;
import com.vicinity24.api.service.PresenceService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UsersController {
    private final UserService userService;
    private final StorageManager storageManager;
    private final PresenceService presenceService;
    private final RuntimeSettingsService runtimeSettingsService;

    public UsersController(UserService userService, StorageManager storageManager, PresenceService presenceService, RuntimeSettingsService runtimeSettingsService) {
        this.userService = userService;
        this.storageManager = storageManager;
        this.presenceService = presenceService;
        this.runtimeSettingsService = runtimeSettingsService;
    }

    private void validateAvatarUpload(MultipartFile file) {
        int maxMb = runtimeSettingsService.getInt("image.max.size.mb", 5);
        long maxBytes = Math.max(1L, (long) maxMb) * 1024L * 1024L;
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file_missing");
        }
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("file_too_large");
        }
        String ct = String.valueOf(file.getContentType() == null ? "" : file.getContentType()).toLowerCase().trim();
        if (!ct.startsWith("image/")) {
            throw new IllegalArgumentException("file_type_not_allowed");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> me(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User u = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(userService.me(u));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteMe(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User u = userService.getByEmail(principal.getUsername());
        userService.deleteUser(u.getId());
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserDTO> updateMe(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @RequestBody UpdateProfileRequest request) {
        System.out.println("UpdateProfileRequest: " + request);
        User u = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(userService.updateProfile(u, request));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @RequestBody ChangePasswordRequest request) {
        User u = userService.getByEmail(principal.getUsername());
        userService.changePassword(u, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDTO> uploadAvatar(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @RequestParam("file") MultipartFile file) throws Exception {
        User u = userService.getByEmail(principal.getUsername());
        validateAvatarUpload(file);
        String key = u.getId().toString() + "/avatar/" + java.util.UUID.randomUUID() + "/" + file.getOriginalFilename();
        String contentType = file.getContentType() == null || file.getContentType().isBlank() ? "application/octet-stream" : file.getContentType();
        String url = storageManager.uploadBytes(key, file.getBytes(), contentType);
        return ResponseEntity.ok(userService.updateAvatar(u, url));
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> all() {
        return ResponseEntity.ok(userService.allUsers());
    }

    @GetMapping("/contacts")
    public ResponseEntity<List<UserSummaryDTO>> contacts(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User u = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(userService.contacts(u));
    }

    @GetMapping("/me/activity")
    public ResponseEntity<List<com.vicinity24.api.dto.ActivityDTO>> activity(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User u = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(userService.getActivity(u));
    }

    @GetMapping("/online")
    public ResponseEntity<List<UUID>> online() {
        return ResponseEntity.ok(presenceService.getOnlineUserIds());
    }

    @PostMapping("/{id}/vouch")
    public ResponseEntity<UserDTO> vouch(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(userService.vouch(id));
    }

    @PostMapping("/verification-request")
    public ResponseEntity<UserDTO> verificationRequest(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @RequestBody Map<String, String> payload) {
        User u = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(userService.verificationRequest(u, payload.get("phone"), payload.get("address")));
    }

    @PostMapping("/{id}/approve-verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> approve(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(userService.approveVerification(id));
    }

    @PostMapping("/{id}/revoke-verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> revoke(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(userService.revokeVerification(id));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> setStatus(@PathVariable("id") UUID id, @RequestBody Map<String, String> payload) {
        UserStatus status = UserStatus.valueOf(payload.get("status"));
        return ResponseEntity.ok(userService.setStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable("id") UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}
