package com.nearshare.api.controller;

import com.nearshare.api.dto.ReturnDTOs;
import com.nearshare.api.model.User;
import com.nearshare.api.service.ReturnService;
import com.nearshare.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/listings/{id}/return")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;
    private final UserService userService;

    @PostMapping("/initiate")
    public ResponseEntity<ReturnDTOs.ReturnSessionResponse> initiateReturn(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User currentUser = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(returnService.initiateReturn(id, currentUser));
    }
    
    @GetMapping
    public ResponseEntity<ReturnDTOs.ReturnSessionResponse> getSession(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User currentUser = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(returnService.getSession(id, currentUser));
    }

    @PostMapping("/scan")
    public ResponseEntity<ReturnDTOs.ReturnSessionResponse> scanQrCode(
            @PathVariable("id") UUID id,
            @RequestBody ReturnDTOs.ScanRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User currentUser = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(returnService.scanQrCode(id, currentUser, request.getQrCode()));
    }

    @PostMapping("/manual")
    public ResponseEntity<ReturnDTOs.ReturnSessionResponse> manualFallback(
            @PathVariable("id") UUID id,
            @RequestBody ReturnDTOs.ManualFallbackRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User currentUser = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(returnService.manualFallback(id, currentUser, request));
    }

    @PostMapping("/dispute")
    public ResponseEntity<ReturnDTOs.ReturnSessionResponse> initiateDispute(
            @PathVariable("id") UUID id,
            @RequestBody ReturnDTOs.DisputeRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User currentUser = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(returnService.initiateDispute(id, currentUser, request));
    }
}
