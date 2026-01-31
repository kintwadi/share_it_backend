package com.nearshare.api.controller;

import com.nearshare.api.dto.LoginRequest;
import com.nearshare.api.dto.RegisterRequest;
import com.nearshare.api.dto.TokenResponse;
import com.nearshare.api.dto.UserDTO;
import com.nearshare.api.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-2fa-login")
    public ResponseEntity<TokenResponse> verify2faLogin(@RequestBody java.util.Map<String, String> body, @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        String code = body.get("code");
        return ResponseEntity.ok(authService.verify2faLogin(principal.getUsername(), code));
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }
}