package com.nearshare.api.controller;

import com.nearshare.api.dto.ChangePasswordRequest;
import com.nearshare.api.dto.UpdateProfileRequest;
import com.nearshare.api.dto.UserDTO;
import com.nearshare.api.dto.UserSummaryDTO;
import com.nearshare.api.model.User;
import com.nearshare.api.model.enums.UserStatus;
import com.nearshare.api.service.UserService;
import com.nearshare.api.storage.StorageManager;
import com.nearshare.api.service.PresenceService;
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

    public UsersController(UserService userService, StorageManager storageManager, PresenceService presenceService) {
        this.userService = userService;
        this.storageManager = storageManager;
        this.presenceService = presenceService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> me(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User u = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(userService.me(u));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserDTO> updateMe(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @RequestBody UpdateProfileRequest request) {
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
        String key = u.getId().toString() + "/avatar/" + java.util.UUID.randomUUID() + "/" + file.getOriginalFilename();
        String url = storageManager.uploadBytes(key, file.getBytes(), file.getContentType());
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
}
