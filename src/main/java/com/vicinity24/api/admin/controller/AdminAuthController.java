package com.vicinity24.api.admin.controller;

import com.vicinity24.api.admin.dto.AdminRegisterRequest;
import com.vicinity24.api.admin.service.AdminAuthService;
import com.vicinity24.api.dto.LoginRequest;
import com.vicinity24.api.dto.TokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        return ResponseEntity.ok(adminAuthService.login(request, httpRequest.getHeader("User-Agent"), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/verify-2fa-login")
    public ResponseEntity<TokenResponse> verify2faLogin(
            @RequestBody java.util.Map<String, String> body,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        String code = body.get("code");
        return ResponseEntity.ok(adminAuthService.verify2faLogin(principal.getUsername(), code, httpRequest.getHeader("User-Agent"), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@RequestBody AdminRegisterRequest request) {
        return ResponseEntity.ok(adminAuthService.register(request));
    }
}
