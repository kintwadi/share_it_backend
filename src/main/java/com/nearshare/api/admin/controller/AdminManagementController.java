package com.nearshare.api.admin.controller;

import com.nearshare.api.admin.dto.AdminDisputeDTO;
import com.nearshare.api.admin.dto.AdminListingDTO;
import com.nearshare.api.admin.dto.AdminPageResponse;
import com.nearshare.api.admin.dto.AdminSubscriptionDTO;
import com.nearshare.api.admin.dto.AdminSummaryDTO;
import com.nearshare.api.admin.dto.AdminTransactionDTO;
import com.nearshare.api.admin.dto.AdminUserDTO;
import com.nearshare.api.admin.service.AdminManagementService;
import com.nearshare.api.model.User;
import com.nearshare.api.model.ReturnSession;
import com.nearshare.api.model.enums.UserStatus;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminManagementController {
    private final AdminManagementService adminService;
    private final com.nearshare.api.service.UserService userService;

    public AdminManagementController(AdminManagementService adminService, com.nearshare.api.service.UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AdminSummaryDTO> summary() {
        return ResponseEntity.ok(adminService.getSummary());
    }

    @GetMapping("/users")
    public ResponseEntity<AdminPageResponse<AdminUserDTO>> users(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.listUsers(q, page, size));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<AdminUserDTO> setUserStatus(@PathVariable("id") UUID userId, @RequestBody UpdateUserStatusRequest body) {
        return ResponseEntity.ok(adminService.setUserStatus(userId, body.getStatus()));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable("id") UUID userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @GetMapping("/transactions")
    public ResponseEntity<AdminPageResponse<AdminTransactionDTO>> transactions(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.listTransactions(status, page, size));
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Map<String, String>> deleteTransaction(@PathVariable("id") UUID transactionId) {
        adminService.deleteTransaction(transactionId);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @PostMapping("/transactions/{id}/retry-release")
    public ResponseEntity<Map<String, String>> retryTransactionRelease(@PathVariable("id") UUID transactionId) {
        adminService.retryTransactionRelease(transactionId);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<AdminPageResponse<AdminSubscriptionDTO>> subscriptions(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.listSubscriptions(status, page, size));
    }

    @GetMapping("/disputes")
    public ResponseEntity<AdminPageResponse<AdminDisputeDTO>> disputes(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.listDisputes(page, size));
    }

    @GetMapping("/listings")
    public ResponseEntity<AdminPageResponse<AdminListingDTO>> listings(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.listListings(status, page, size));
    }

    @GetMapping("/partner/listing-requests")
    public ResponseEntity<AdminPageResponse<AdminListingDTO>> partnerListingRequests(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.listPartnerListingRequests(page, size));
    }

    @PostMapping("/partner/listings/{listingId}/approve")
    public ResponseEntity<Map<String, String>> approvePartnerListingScoped(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @PathVariable("listingId") UUID listingId,
            @RequestBody(required = false) ApprovePartnerListingRequest body) {
        User admin = userService.getByEmail(principal.getUsername());
        adminService.approvePartnerListing(admin, listingId, body != null ? body.getNote() : null);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/partner/listings/{listingId}/reject")
    public ResponseEntity<Map<String, String>> rejectPartnerListingScoped(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @PathVariable("listingId") UUID listingId,
            @RequestBody(required = false) RejectPartnerListingRequest body) {
        User admin = userService.getByEmail(principal.getUsername());
        adminService.rejectPartnerListing(admin, listingId, body != null ? body.getReason() : null);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/disputes/{listingId}/cancel-refund")
    public ResponseEntity<Map<String, String>> cancelAndRefund(@PathVariable("listingId") UUID listingId, @RequestBody ResolveDisputeRequest body) {
        adminService.cancelAndRefundDispute(listingId, body.getReason());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/returns/{listingId}/accept")
    public ResponseEntity<Map<String, String>> acceptReturn(@PathVariable("listingId") UUID listingId, @RequestBody ResolveDisputeRequest body) {
        adminService.acceptReturnDispute(listingId, body.getReason());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/returns/{listingId}/reopen")
    public ResponseEntity<Map<String, Object>> reopenReturn(@PathVariable("listingId") UUID listingId, @RequestBody ReopenReturnRequest body) {
        ReturnSession session = adminService.reopenReturnSession(listingId, body != null ? body.getMinutes() : null);
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "returnSessionId", session.getId(),
                "expiresAt", session.getExpiresAt()
        ));
    }

    @PostMapping("/listings/{listingId}/block")
    public ResponseEntity<Map<String, String>> blockListing(@PathVariable("listingId") UUID listingId, @RequestBody BlockListingRequest body) {
        adminService.blockListing(listingId, Boolean.TRUE.equals(body.getBlocked()));
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @DeleteMapping("/listings/{listingId}")
    public ResponseEntity<Map<String, String>> deleteListing(@PathVariable("listingId") UUID listingId) {
        adminService.deleteListing(listingId);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @PostMapping("/listings/{listingId}/approve")
    public ResponseEntity<Map<String, String>> approvePartnerListing(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @PathVariable("listingId") UUID listingId,
            @RequestBody(required = false) ApprovePartnerListingRequest body) {
        User admin = userService.getByEmail(principal.getUsername());
        adminService.approvePartnerListing(admin, listingId, body != null ? body.getNote() : null);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/listings/{listingId}/reject")
    public ResponseEntity<Map<String, String>> rejectPartnerListing(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @PathVariable("listingId") UUID listingId,
            @RequestBody(required = false) RejectPartnerListingRequest body) {
        User admin = userService.getByEmail(principal.getUsername());
        adminService.rejectPartnerListing(admin, listingId, body != null ? body.getReason() : null);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @Data
    public static class UpdateUserStatusRequest {
        private UserStatus status;
    }

    @Data
    public static class BlockListingRequest {
        private Boolean blocked;
    }

    @Data
    public static class ResolveDisputeRequest {
        private String reason;
    }

    @Data
    public static class ReopenReturnRequest {
        private Integer minutes;
    }

    @Data
    public static class ApprovePartnerListingRequest {
        private String note;
    }

    @Data
    public static class RejectPartnerListingRequest {
        private String reason;
    }
}
