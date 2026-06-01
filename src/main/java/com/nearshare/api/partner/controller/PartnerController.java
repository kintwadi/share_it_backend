package com.nearshare.api.partner.controller;

import com.nearshare.api.model.User;
import com.nearshare.api.partner.dto.PartnerBorrowRequestDTO;
import com.nearshare.api.partner.dto.PartnerCreateListingRequest;
import com.nearshare.api.partner.dto.PartnerDTO;
import com.nearshare.api.partner.dto.PartnerRegistrationRequest;
import com.nearshare.api.partner.dto.PartnerReturnRequestDTO;
import com.nearshare.api.partner.dto.PartnerSettingsDTO;
import com.nearshare.api.partner.service.PartnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/partner")
public class PartnerController {
    private final PartnerService partnerService;
    private final com.nearshare.api.service.UserService userService;

    public PartnerController(PartnerService partnerService, com.nearshare.api.service.UserService userService) {
        this.partnerService = partnerService;
        this.userService = userService;
    }

    @GetMapping("/my-partners")
    public ResponseEntity<List<PartnerDTO>> myPartners(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User current = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(partnerService.getMyPartners(current));
    }

    @PostMapping("/register")
    public ResponseEntity<PartnerDTO> register(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody PartnerRegistrationRequest req) {
        User current = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(partnerService.registerPartner(current, req));
    }

    @PostMapping("/listings")
    public ResponseEntity<com.nearshare.api.dto.ListingDTO> createListing(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody PartnerCreateListingRequest req) {
        User current = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(partnerService.createPartnerListing(current, req));
    }

    @GetMapping("/listings")
    public ResponseEntity<List<com.nearshare.api.dto.ListingDTO>> listings(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User current = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(partnerService.getPartnerListings(current));
    }

    @PutMapping("/listings/{listingId}")
    public ResponseEntity<com.nearshare.api.dto.ListingDTO> updateListing(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @PathVariable("listingId") UUID listingId,
            @RequestBody PartnerCreateListingRequest req) {
        User current = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(partnerService.updatePartnerListing(current, listingId, req));
    }

    @DeleteMapping("/listings/{listingId}")
    public ResponseEntity<Map<String, String>> deleteListing(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @PathVariable("listingId") UUID listingId) {
        User current = userService.getByEmail(principal.getUsername());
        partnerService.deletePartnerListing(current, listingId);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<PartnerBorrowRequestDTO>> requests(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User current = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(partnerService.getPartnerRequests(current));
    }

    @PostMapping("/requests/{listingId}/approve")
    public ResponseEntity<com.nearshare.api.dto.ListingDTO> approveRequest(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @PathVariable("listingId") UUID listingId) {
        User current = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(partnerService.approvePartnerRequest(current, listingId));
    }

    @PostMapping("/requests/{listingId}/reject")
    public ResponseEntity<com.nearshare.api.dto.ListingDTO> rejectRequest(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @PathVariable("listingId") UUID listingId) {
        User current = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(partnerService.rejectPartnerRequest(current, listingId));
    }

    @GetMapping("/returns/manual/pending")
    public ResponseEntity<List<PartnerReturnRequestDTO>> pendingManualReturns(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User current = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(partnerService.getPendingManualReturns(current));
    }

    @PostMapping("/returns/{listingId}/accept")
    public ResponseEntity<com.nearshare.api.dto.ReturnDTOs.ReturnSessionResponse> acceptManualReturn(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @PathVariable("listingId") UUID listingId) {
        User current = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(partnerService.acceptManualReturn(current, listingId));
    }

    @PostMapping("/returns/{listingId}/deny")
    public ResponseEntity<com.nearshare.api.dto.ReturnDTOs.ReturnSessionResponse> denyManualReturn(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @PathVariable("listingId") UUID listingId,
            @RequestBody(required = false) Map<String, String> body) {
        User current = userService.getByEmail(principal.getUsername());
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(partnerService.denyManualReturn(current, listingId, reason));
    }

    @GetMapping("/settings")
    public ResponseEntity<PartnerSettingsDTO> getSettings(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestParam(name = "partnerId", required = false) UUID partnerId) {
        User current = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(partnerService.getSettings(current, partnerId));
    }

    @PutMapping("/settings")
    public ResponseEntity<PartnerSettingsDTO> updateSettings(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestParam(name = "partnerId", required = false) UUID partnerId,
            @RequestBody PartnerSettingsDTO dto) {
        User current = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(partnerService.updateSettings(current, partnerId, dto));
    }
}
