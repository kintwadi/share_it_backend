package com.vicinity24.api.controller;

import com.vicinity24.api.model.User;
import com.vicinity24.api.service.EncryptionService;
import com.vicinity24.api.service.TwoFactorService;
import com.vicinity24.api.service.UserService;
import com.vicinity24.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/2fa")
@RequiredArgsConstructor
public class TwoFactorController {
    private final UserService userService;
    private final TwoFactorService twoFactorService;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    @PostMapping("/setup")
    public ResponseEntity<Map<String, String>> setup(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        String secret = twoFactorService.generateSecret();
        // Encrypt the secret before storing
        user.setTwoFactorSecret(encryptionService.encrypt(secret));
        userRepository.save(user);
        
        String qrCode = twoFactorService.generateQrCodeImageUri(secret, user.getEmail());
        return ResponseEntity.ok(Map.of("secret", secret, "qrCode", qrCode));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @RequestBody Map<String, String> payload) {
        User user = userService.getByEmail(principal.getUsername());
        String code = payload.get("code");
        String secret = user.getTwoFactorSecret();
        
        if (secret == null) {
            return ResponseEntity.badRequest().body("2FA not set up");
        }

        if (twoFactorService.verify(secret, code)) {
            user.setTwoFactorEnabled(true);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true));
        } else {
            return ResponseEntity.badRequest().body("Invalid code");
        }
    }

    @PostMapping("/disable")
    public ResponseEntity<?> disable(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
