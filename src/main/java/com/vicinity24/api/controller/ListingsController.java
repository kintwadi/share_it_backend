package com.vicinity24.api.controller;

import com.vicinity24.api.dto.CreateListingRequest;
import com.vicinity24.api.dto.ListingDTO;
import com.vicinity24.api.dto.ReportRequest;
import com.vicinity24.api.model.User;
import com.vicinity24.api.service.ListingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings")
public class ListingsController {
    private static final Logger logger = LoggerFactory.getLogger(ListingsController.class);
    private final ListingService listingService;
    private final com.vicinity24.api.service.UserService userService;

    public ListingsController(ListingService listingService, com.vicinity24.api.service.UserService userService) {
        this.listingService = listingService;
        this.userService = userService;
    }

    @GetMapping("/")
    public ResponseEntity<Page<ListingDTO>> list(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            HttpServletRequest request,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "minPrice", required = false) Double minPrice,
            @RequestParam(name = "viewerLat", required = false) Double viewerLat,
            @RequestParam(name = "viewerLng", required = false) Double viewerLng,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        User current = principal != null ? userService.getByEmail(principal.getUsername()) : null;
        return ResponseEntity.ok(listingService.findAll(current, search, category, type, minPrice, page, size, viewerLat, viewerLng));
    }

    @GetMapping("/nearby")
    public ResponseEntity<java.util.List<ListingDTO>> nearby(
            @RequestParam(name = "lat") double lat,
            @RequestParam(name = "lng") double lng,
            @RequestParam(name = "radiusKm", defaultValue = "25") double radiusKm,
            @RequestParam(name = "size", defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(listingService.findNearby(lat, lng, radiusKm, size));
    }

    @GetMapping("/recommended")
    public ResponseEntity<java.util.List<ListingDTO>> recommended(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestParam(name = "size", defaultValue = "6") int size) {
        User current = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(listingService.recommended(current, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingDTO> get(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @PathVariable("id") UUID id) {
        User current = principal != null ? userService.getByEmail(principal.getUsername()) : null;
        return ResponseEntity.ok(listingService.getById(id, current));
    }

    @PostMapping("/")
    public ResponseEntity<ListingDTO> create(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, HttpServletRequest request, @RequestBody CreateListingRequest req) {
        logger.info(
                "POST /api/listings/ principal={} type={} category={} pickupLocationId={} availableUnlimited={} availableFrom={} hasImage={} galleryCount={}",
                principal != null ? principal.getUsername() : "anonymous",
                req != null ? req.getType() : null,
                req != null ? req.getCategory() : null,
                req != null ? req.getPickupLocationId() : null,
                req != null && req.isAvailableUnlimited(),
                req != null ? req.getAvailableFrom() : null,
                req != null && req.getImageUrl() != null && !req.getImageUrl().isBlank(),
                req != null && req.getGallery() != null ? req.getGallery().size() : 0
        );
        User owner = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(listingService.create(owner, req, null, null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListingDTO> update(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @PathVariable("id") UUID id, @RequestBody CreateListingRequest req) {
        User current = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(listingService.update(id, req, current));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @PathVariable("id") UUID id) {
        User current = userService.getByEmail(principal.getUsername());
        listingService.delete(id, current);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @PostMapping("/{id}/borrow")
    public ResponseEntity<ListingDTO> borrow(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, 
            @PathVariable("id") UUID id,
            @RequestBody com.vicinity24.api.dto.BorrowRequest request) {
        User borrower = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(listingService.borrow(id, borrower, request));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ListingDTO> approve(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @PathVariable("id") UUID id) {
        User owner = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(listingService.approve(id, owner));
    }

    @PostMapping("/{id}/deny")
    public ResponseEntity<ListingDTO> deny(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @PathVariable("id") UUID id) {
        User owner = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(listingService.deny(id, owner));
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<ListingDTO> returnItem(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @PathVariable("id") UUID id) {
        User owner = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(listingService.returnItem(id, owner));
    }

    @PostMapping("/{id}/ready-for-pickup")
    public ResponseEntity<ListingDTO> readyForPickup(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @PathVariable("id") UUID id) {
        User current = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(listingService.markReadyForPickup(id, current));
    }

    @PostMapping("/{id}/picked-up")
    public ResponseEntity<ListingDTO> pickedUp(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @PathVariable("id") UUID id) {
        User current = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(listingService.markPickedUp(id, current));
    }

    @PostMapping("/{id}/request-admin-return")
    public ResponseEntity<ListingDTO> requestAdminReturn(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal, @PathVariable("id") UUID id) {
        User current = userService.getByEmail(principal.getUsername());
        return ResponseEntity.ok(listingService.requestAdminReturn(id, current));
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ListingDTO> block(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(listingService.block(id));
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<Map<String, String>> report(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @PathVariable("id") UUID id,
            @RequestBody ReportRequest req) {
        User reporter = userService.getByEmail(principal.getUsername());
        listingService.report(id, reporter, req.getReason(), req.getDetails());
        return ResponseEntity.ok(Map.of("status", "reported"));
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<java.util.Map<String, String>> dismiss(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @PathVariable("id") UUID id) {
        User current = userService.getByEmail(principal.getUsername());
        listingService.dismiss(current, id);
        return ResponseEntity.ok(java.util.Map.of("status", "dismissed"));
    }
}
