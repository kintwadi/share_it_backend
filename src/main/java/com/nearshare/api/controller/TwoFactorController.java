package com.nearshare.api.controller;

import com.nearshare.api.model.User;
import com.nearshare.api.service.TwoFactorService;
import com.nearshare.api.service.UserService;
import com.nearshare.api.repository.UserRepository;
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

    @PostMapping("/setup")
    public ResponseEntity<Map<String, String>> setup(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        String secret = twoFactorService.generateSecret();
        user.setTwoFactorSecret(secret);
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
