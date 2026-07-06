package com.vicinity24.api.bicycle.controller;

import com.vicinity24.api.bicycle.dto.FahradFuchsBookingDto;
import com.vicinity24.api.bicycle.dto.FahradFuchsCheckoutRequest;
import com.vicinity24.api.bicycle.dto.FahradFuchsCheckoutResponse;
import com.vicinity24.api.bicycle.dto.FahradFuchsListingDetailDto;
import com.vicinity24.api.bicycle.dto.FahradFuchsStoreDto;
import com.vicinity24.api.bicycle.dto.FahradFuchsStorefrontDto;
import com.vicinity24.api.bicycle.service.FahradFuchsStorefrontService;
import com.vicinity24.api.core.model.User;
import com.vicinity24.api.core.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/fahrad-fuchs")
public class FahradFuchsController {

    private final FahradFuchsStorefrontService storefrontService;
    private final UserService userService;

    public FahradFuchsController(FahradFuchsStorefrontService storefrontService, UserService userService) {
        this.storefrontService = storefrontService;
        this.userService = userService;
    }

    @GetMapping("/store")
    public ResponseEntity<FahradFuchsStoreDto> store() {
        return ResponseEntity.ok(storefrontService.storefront().store());
    }

    @GetMapping("/bikes")
    public ResponseEntity<FahradFuchsStorefrontDto> bikes() {
        return ResponseEntity.ok(storefrontService.storefront());
    }

    @GetMapping("/bikes/{slug}")
    public ResponseEntity<FahradFuchsListingDetailDto> bike(@PathVariable("slug") String slug) {
        return ResponseEntity.ok(storefrontService.getBike(slug));
    }

    @PostMapping("/bikes/{slug}/checkout")
    public ResponseEntity<FahradFuchsCheckoutResponse> checkout(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @PathVariable("slug") String slug,
            @Valid @RequestBody FahradFuchsCheckoutRequest request
    ) {
        User borrower = requireAuthenticatedUser(principal);
        return ResponseEntity.ok(storefrontService.checkout(slug, request, borrower));
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<FahradFuchsBookingDto>> bookings(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal
    ) {
        User borrower = requireAuthenticatedUser(principal);
        return ResponseEntity.ok(storefrontService.getBookings(borrower));
    }

    private User requireAuthenticatedUser(org.springframework.security.core.userdetails.User principal) {
        if (principal == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "authentication_required");
        }
        return userService.getByEmail(principal.getUsername());
    }
}
