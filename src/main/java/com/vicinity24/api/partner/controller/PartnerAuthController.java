package com.vicinity24.api.partner.controller;

import com.vicinity24.api.dto.LoginRequest;
import com.vicinity24.api.dto.TokenResponse;
import com.vicinity24.api.partner.dto.PartnerAuthRegisterRequest;
import com.vicinity24.api.partner.service.PartnerAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/partner/auth")
public class PartnerAuthController {
    private final PartnerAuthService partnerAuthService;

    public PartnerAuthController(PartnerAuthService partnerAuthService) {
        this.partnerAuthService = partnerAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        return ResponseEntity.ok(partnerAuthService.login(request, httpRequest.getHeader("User-Agent"), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/verify-2fa-login")
    public ResponseEntity<TokenResponse> verify2faLogin(
            @RequestBody java.util.Map<String, String> body,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        String code = body.get("code");
        return ResponseEntity.ok(partnerAuthService.verify2faLogin(principal.getUsername(), code, httpRequest.getHeader("User-Agent"), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@RequestBody PartnerAuthRegisterRequest request) {
        return ResponseEntity.ok(partnerAuthService.register(request));
    }
}
