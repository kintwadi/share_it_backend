package com.vicinity24.api.controller;

import com.vicinity24.api.payment.StripePayment;
import com.vicinity24.api.config.RuntimeSettingsService;
import com.vicinity24.api.repository.ListingRepository;
import com.vicinity24.api.model.Listing;
import com.vicinity24.api.dto.PaymentTransactionDTO;
import com.vicinity24.api.service.EscrowService;
import com.vicinity24.api.service.ListingService;
import com.vicinity24.api.service.SubscriptionService;
import com.vicinity24.api.service.UserService;
import com.vicinity24.api.model.User;
import com.vicinity24.api.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.stripe.model.Event;
import com.stripe.model.Account;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.Charge;
import com.stripe.model.checkout.Session;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final StripePayment stripePayment;
    private final ListingService listingService;
    private final ListingRepository listingRepository;
    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final RuntimeSettingsService runtimeSettingsService;
    private final EscrowService escrowService;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${subscription.plus.stripe_price_id:}")
    private String plusStripePriceId;

    @org.springframework.beans.factory.annotation.Value("${subscription.pro.stripe_price_id:}")
    private String proStripePriceId;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.baseUrl:http://localhost:3001}")
    private String frontendBaseUrl;

    @org.springframework.beans.factory.annotation.Value("${settings.service.fee_percent:0.08}")
    private double configuredServiceFeePercent;

    public PaymentController(StripePayment stripePayment, ListingService listingService, ListingRepository listingRepository, UserService userService, SubscriptionService subscriptionService, RuntimeSettingsService runtimeSettingsService, EscrowService escrowService, TransactionRepository transactionRepository, ObjectMapper objectMapper) {
        this.stripePayment = stripePayment;
        this.listingService = listingService;
        this.listingRepository = listingRepository;
        this.userService = userService;
        this.subscriptionService = subscriptionService;
        this.runtimeSettingsService = runtimeSettingsService;
        this.escrowService = escrowService;
        this.transactionRepository = transactionRepository;
        this.objectMapper = objectMapper;
    }

    private String resolveSubscriptionPlanType(String priceId) {
        if (priceId == null || priceId.isBlank()) return null;
        String effectivePlusPriceId = runtimeSettingsService != null
                ? runtimeSettingsService.getString("subscription.plus.stripe_price_id", plusStripePriceId)
                : plusStripePriceId;
        String effectiveProPriceId = runtimeSettingsService != null
                ? runtimeSettingsService.getString("subscription.pro.stripe_price_id", proStripePriceId)
                : proStripePriceId;
        if (priceId.equals(effectivePlusPriceId)) return "plus";
        if (priceId.equals(effectiveProPriceId)) return "pro";
        return null;
    }

    private BigDecimal resolveServiceFeeRate() {
        double rate = runtimeSettingsService != null
                ? runtimeSettingsService.getDouble("settings.service.fee_percent", configuredServiceFeePercent)
                : configuredServiceFeePercent;
        if (!Double.isFinite(rate) || rate < 0) {
            rate = configuredServiceFeePercent;
        }
        return BigDecimal.valueOf(rate);
    }

    @GetMapping("/methods")
    public ResponseEntity<Object> listPaymentMethods(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "unauthorized"));
        }
        User user = userService.getByEmail(principal.getUsername());
        if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        try {
            return ResponseEntity.ok(stripePayment.listPaymentMethods(user.getStripeCustomerId()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "request_failed"));
        }
    }

    @PostMapping("/methods")
    public ResponseEntity<Map<String, String>> addPaymentMethod(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody Map<String, String> payload
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "unauthorized"));
        }
        String paymentMethodId = payload != null ? payload.get("paymentMethodId") : null;
        if (paymentMethodId == null || paymentMethodId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "payment_method_id_required"));
        }
        User user = userService.getByEmail(principal.getUsername());
        String customerId = user.getStripeCustomerId();
        if (customerId == null || customerId.isBlank()) {
            customerId = stripePayment.createCustomer(user.getEmail(), user.getName());
            user.setStripeCustomerId(customerId);
            userService.save(user);
        }
        try {
            stripePayment.attachPaymentMethod(customerId, paymentMethodId);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "request_failed"));
        }
    }

    @DeleteMapping("/methods/{id}")
    public ResponseEntity<Map<String, String>> deletePaymentMethod(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @PathVariable("id") String paymentMethodId
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "unauthorized"));
        }
        if (paymentMethodId == null || paymentMethodId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "payment_method_id_required"));
        }
        User user = userService.getByEmail(principal.getUsername());
        String customerId = user.getStripeCustomerId();
        if (customerId == null || customerId.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
        }
        try {
            PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
            String pmCustomer = pm != null ? pm.getCustomer() : null;
            if (pmCustomer == null || !pmCustomer.equals(customerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
            }
            stripePayment.detachPaymentMethod(paymentMethodId);
            return ResponseEntity.ok(Map.of("status", "deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "request_failed"));
        }
    }

    @PostMapping("/connect/onboard")
    public ResponseEntity<Map<String, Object>> connectOnboard(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        if (user.getStripeConnectAccountId() == null || user.getStripeConnectAccountId().isBlank()) {
            String acctId = stripePayment.createExpressConnectAccount(user.getEmail(), user.getName());
            user.setStripeConnectAccountId(acctId);
            userService.save(user);
        }

        String refreshUrl = frontendBaseUrl + "/settings?tab=payments&connect=refresh";
        String returnUrl = frontendBaseUrl + "/settings?tab=payments&connect=return";
        String url = stripePayment.createAccountOnboardingLink(user.getStripeConnectAccountId(), refreshUrl, returnUrl);
        return ResponseEntity.ok(Map.of(
                "accountId", user.getStripeConnectAccountId(),
                "url", url
        ));
    }

    @GetMapping("/connect/status")
    public ResponseEntity<Map<String, Object>> connectStatus(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        String acctId = user.getStripeConnectAccountId();
        if (acctId == null || acctId.isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "connected", false
            ));
        }
        Account acct = stripePayment.retrieveAccount(acctId);
        boolean detailsSubmitted = acct != null && acct.getDetailsSubmitted() != null && acct.getDetailsSubmitted();
        user.setStripeConnectDetailsSubmitted(detailsSubmitted);
        userService.save(user);
        int attemptedReleases = 0;
        if (detailsSubmitted && user.getId() != null) {
            attemptedReleases = escrowService.retryFailedReleasesForPayee(user.getId());
        }
        String transfersCapability = null;
        if (acct != null && acct.getCapabilities() != null) {
            transfersCapability = acct.getCapabilities().getTransfers();
        }
        List<String> currentlyDue = acct != null && acct.getRequirements() != null ? acct.getRequirements().getCurrentlyDue() : null;
        String disabledReason = acct != null && acct.getRequirements() != null ? acct.getRequirements().getDisabledReason() : null;
        Map<String, Object> out = new HashMap<>();
        out.put("connected", true);
        out.put("accountId", acctId);
        out.put("detailsSubmitted", detailsSubmitted);
        out.put("chargesEnabled", acct != null && acct.getChargesEnabled() != null ? acct.getChargesEnabled() : false);
        out.put("payoutsEnabled", acct != null && acct.getPayoutsEnabled() != null ? acct.getPayoutsEnabled() : false);
        out.put("attemptedReleaseRetries", attemptedReleases);
        if (transfersCapability != null) {
            out.put("transfersCapability", transfersCapability);
        }
        if (currentlyDue != null) {
            out.put("requirementsCurrentlyDue", currentlyDue);
        }
        if (disabledReason != null) {
            out.put("requirementsDisabledReason", disabledReason);
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<PaymentTransactionDTO>> getMyTransactions(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        UUID userId = user.getId();

        List<com.vicinity24.api.model.Transaction> payerTx = transactionRepository.findByPayerIdOrderByTimestampDesc(userId);
        List<com.vicinity24.api.model.Transaction> payeeTx = transactionRepository.findByPayeeIdOrderByTimestampDesc(userId);

        java.util.Map<UUID, PaymentTransactionDTO> merged = new java.util.HashMap<>();

        for (com.vicinity24.api.model.Transaction t : payerTx) {
            if (t == null || t.getId() == null) continue;
            merged.put(t.getId(), toPaymentTxDTO(t, userId));
        }
        for (com.vicinity24.api.model.Transaction t : payeeTx) {
            if (t == null || t.getId() == null) continue;
            merged.put(t.getId(), toPaymentTxDTO(t, userId));
        }

        List<PaymentTransactionDTO> out = merged.values().stream()
                .sorted((a, b) -> {
                    if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
                    if (a.getTimestamp() == null) return 1;
                    if (b.getTimestamp() == null) return -1;
                    return b.getTimestamp().compareTo(a.getTimestamp());
                })
                .toList();

        return ResponseEntity.ok(out);
    }

    @GetMapping("/transactions/{id}/invoice")
    public ResponseEntity<Map<String, String>> getTransactionInvoiceUrl(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @PathVariable("id") UUID transactionId
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "unauthorized"));
        }
        User user = userService.getByEmail(principal.getUsername());

        com.vicinity24.api.model.Transaction tx = transactionRepository.findById(transactionId).orElse(null);
        if (tx == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "transaction_not_found"));
        }

        UUID userId = user.getId();
        boolean isParty = (tx.getPayer() != null && tx.getPayer().getId() != null && tx.getPayer().getId().equals(userId))
                || (tx.getPayee() != null && tx.getPayee().getId() != null && tx.getPayee().getId().equals(userId));
        boolean isAdmin = user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().name());
        if (!isParty && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
        }

        if (!"RELEASED".equalsIgnoreCase(tx.getStatus()) && !"REFUNDED".equalsIgnoreCase(tx.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "not_released"));
        }
        if (tx.getPaymentToken() == null || tx.getPaymentToken().isBlank() || !tx.getPaymentToken().startsWith("pi_")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "invoice_unavailable"));
        }

        try {
            PaymentIntent intent = PaymentIntent.retrieve(tx.getPaymentToken());
            String chargeId = intent != null ? intent.getLatestCharge() : null;
            if (chargeId == null || chargeId.isBlank()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "invoice_unavailable"));
            }
            Charge charge = Charge.retrieve(chargeId);
            String receiptUrl = charge != null ? charge.getReceiptUrl() : null;
            if (receiptUrl == null || receiptUrl.isBlank()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "invoice_unavailable"));
            }
            return ResponseEntity.ok(Map.of("url", receiptUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "invoice_unavailable"));
        }
    }

    @PostMapping("/release/retry")
    public ResponseEntity<Map<String, Object>> retryMyReleaseFailures(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        User user = userService.getByEmail(principal.getUsername());
        int attempted = escrowService.retryFailedReleasesForPayee(user.getId());
        return ResponseEntity.ok(Map.of(
                "attempted", attempted
        ));
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, Object>> createPaymentIntent(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody Map<String, Object> payload) {
        
        String listingId = (String) payload.get("listingId");
        String borrowerPath = payload.containsKey("borrowerPath") ? String.valueOf(payload.get("borrowerPath")) : "VERIFIED";
        String paymentMethodId = payload.containsKey("paymentMethodId") ? String.valueOf(payload.get("paymentMethodId")) : null;
        
        // Optional duration
        String durationHours = payload.containsKey("durationHours") ? payload.get("durationHours").toString() : "0";
        int duration = 0;
        try {
            duration = Integer.parseInt(durationHours);
        } catch (Exception ignored) {
            duration = 0;
        }

        User user = userService.getByEmail(principal.getUsername());

        Listing listing = listingRepository.findById(java.util.UUID.fromString(listingId))
                .orElseThrow(() -> new RuntimeException("listing_not_found"));

        if (listing.getPartner() != null) {
            throw new RuntimeException("partner_listing_offline_payment");
        }

        if (listing.getType() == com.vicinity24.api.model.enums.ListingType.GIVE) {
            throw new RuntimeException("free_listing_no_payment_required");
        }

        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal hourlyRate = listing.getHourlyRate() != null ? listing.getHourlyRate() : BigDecimal.ZERO;
        boolean isTimeBased = listing.getType() != com.vicinity24.api.model.enums.ListingType.GIVE && listing.getType() != com.vicinity24.api.model.enums.ListingType.SELL;
        int effectiveDuration = isTimeBased ? (duration > 0 ? duration : 1) : 1;
        BigDecimal totalCost = hourlyRate.multiply(BigDecimal.valueOf(effectiveDuration));
        BigDecimal serviceFee = BigDecimal.ZERO;
        BigDecimal depositAmount = BigDecimal.ZERO;
        String bp = borrowerPath != null ? borrowerPath.toUpperCase() : "VERIFIED";
        if ("FEE".equals(bp)) {
            serviceFee = totalCost.multiply(resolveServiceFeeRate()).setScale(2, java.math.RoundingMode.HALF_UP);
        } else if ("DEPOSIT".equals(bp)) {
            depositAmount = new BigDecimal("50.00");
        }
        amount = totalCost.add(serviceFee).add(depositAmount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("free_listing_no_payment_required");
        }
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("listingId", listingId);
        metadata.put("borrowerId", user.getId().toString());
        metadata.put("durationHours", durationHours);
        metadata.put("borrowerPath", borrowerPath);
        metadata.put("borrowerEmail", user.getEmail());
        metadata.put("borrowerName", user.getName());

        String customerId = user.getStripeCustomerId();
        if (paymentMethodId != null && !paymentMethodId.isBlank() && (customerId == null || customerId.isBlank())) {
            customerId = stripePayment.createCustomer(user.getEmail(), user.getName());
            user.setStripeCustomerId(customerId);
            userService.save(user);
        }
        PaymentIntent intent = stripePayment.createPaymentIntent(amount, "usd", customerId, metadata);
        
        return ResponseEntity.ok(Map.of(
                "clientSecret", intent.getClientSecret(),
                "amount", amount,
                "currency", "usd"
        ));
    }

    private PaymentTransactionDTO toPaymentTxDTO(com.vicinity24.api.model.Transaction t, UUID currentUserId) {
        UUID payerId = t.getPayer() != null ? t.getPayer().getId() : null;
        UUID payeeId = t.getPayee() != null ? t.getPayee().getId() : null;
        String direction = payerId != null && payerId.equals(currentUserId) ? "MADE" : "RECEIVED";

        UUID listingId = t.getListing() != null ? t.getListing().getId() : null;
        String listingTitle = t.getListing() != null ? t.getListing().getTitle() : null;

        return PaymentTransactionDTO.builder()
                .id(t.getId())
                .listingId(listingId)
                .listingTitle(listingTitle)
                .payerId(payerId)
                .payeeId(payeeId)
                .direction(direction)
                .amount(t.getAmount())
                .rentalAmount(t.getRentalAmount())
                .serviceFeeAmount(t.getServiceFeeAmount())
                .depositAmount(t.getDepositAmount())
                .currency(t.getCurrency())
                .paymentMethod(t.getPaymentMethod())
                .status(t.getStatus())
                .releaseError(t.getReleaseError())
                .timestamp(t.getTimestamp())
                .build();
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = stripePayment.constructWebhookEvent(payload, sigHeader);

            log.info("Stripe webhook received: type={}, id={}", event.getType(), event.getId());

            if ("payment_intent.succeeded".equals(event.getType())) {
                StripeObject dataObject = event.getDataObjectDeserializer().getObject().orElse(null);
                if (dataObject instanceof PaymentIntent) {
                    PaymentIntent intent = (PaymentIntent) dataObject;
                    log.info("payment_intent.succeeded: id={}, amount={}, currency={}",
                            intent.getId(), intent.getAmount(), intent.getCurrency());
                    Map<String, String> metadata = intent.getMetadata();
                    
                    if (metadata != null && metadata.containsKey("listingId") && metadata.containsKey("borrowerId")) {
                        String listingId = metadata.get("listingId");
                        String borrowerId = metadata.get("borrowerId");
                        String durationStr = metadata.getOrDefault("durationHours", "0");
                        String borrowerPath = metadata.getOrDefault("borrowerPath", "VERIFIED");
                        int duration = Integer.parseInt(durationStr);
                        BigDecimal amount = BigDecimal.valueOf(intent.getAmount()).divide(new BigDecimal(100));
                        
                        listingService.completeTransaction(intent.getId(), listingId, borrowerId, borrowerPath, amount, duration);
                    }
                } else {
                    log.warn("payment_intent.succeeded data object not PaymentIntent, class={}", dataObject != null ? dataObject.getClass() : null);
                }
            }
            if ("checkout.session.completed".equals(event.getType())) {
                StripeObject dataObject = event.getDataObjectDeserializer().getObject().orElse(null);
                if (dataObject instanceof Session) {
                    Session session = (Session) dataObject;
                    String subscriptionId = session.getSubscription();
                    Map<String, String> metadata = session.getMetadata();
                    log.info("checkout.session.completed: sessionId={}, subscriptionId={}",
                            session.getId(), subscriptionId);
                    if (subscriptionId != null && !subscriptionId.isBlank() && metadata != null && metadata.containsKey("app_user_id")) {
                        String uid = metadata.get("app_user_id");
                        try {
                            java.util.UUID uuid = java.util.UUID.fromString(uid);
                            User user = userService.getById(uuid);
                            com.stripe.model.Subscription stripeSub = Subscription.retrieve(subscriptionId);
                            String status = stripeSub.getStatus();
                            
                            String planType = null;
                            if (stripeSub.getItems() != null && stripeSub.getItems().getData() != null && !stripeSub.getItems().getData().isEmpty()) {
                                 String priceId = stripeSub.getItems().getData().get(0).getPrice().getId();
                                 planType = resolveSubscriptionPlanType(priceId);
                            }
                            
                            log.info("Syncing subscription from checkout.session.completed: subId={}, status={}, userId={}, plan={}",
                                    subscriptionId, status, user.getId(), planType);
                            subscriptionService.syncProSubscriptionFromStripe(user, subscriptionId, status, planType);
                        } catch (IllegalArgumentException e) {
                            log.warn("Invalid app_user_id format in session metadata: {}", uid);
                        } catch (Exception e) {
                            log.error("Error syncing subscription from checkout.session.completed for subscription {}", subscriptionId, e);
                        }
                    } else {
                        log.warn("checkout.session.completed missing subscriptionId or app_user_id metadata");
                    }
                } else {
                    log.warn("checkout.session.completed data object not Session, class={}", dataObject != null ? dataObject.getClass() : null);
                }
            }
            if ("invoice.payment_succeeded".equals(event.getType())) {
                try {
                    JsonNode root = objectMapper.readTree(payload);
                    JsonNode invoiceNode = root.path("data").path("object");
                    String subscriptionId = invoiceNode.path("subscription").asText(null);
                    long createdEpoch = invoiceNode.path("created").asLong(0L);
                    String invoicePdfUrl = invoiceNode.path("invoice_pdf").asText(null);
                    java.time.LocalDateTime invoiceDate;
                    if (createdEpoch > 0L) {
                        invoiceDate = java.time.LocalDateTime.ofEpochSecond(createdEpoch, 0, java.time.ZoneOffset.UTC);
                    } else {
                        invoiceDate = java.time.LocalDateTime.now();
                    }
                    log.info("invoice.payment_succeeded JSON parsed: subscriptionId={}, created={}", subscriptionId, createdEpoch);
                    if (subscriptionId != null && !subscriptionId.isBlank()) {
                        subscriptionService.updateInvoiceInfoFromStripe(subscriptionId, invoicePdfUrl, invoiceDate);
                    } else {
                        log.warn("invoice.payment_succeeded without subscription id in JSON");
                    }
                } catch (Exception e) {
                    log.error("Failed to parse invoice.payment_succeeded JSON", e);
                }
            }
            if ("customer.subscription.created".equals(event.getType())
                    || "customer.subscription.updated".equals(event.getType())
                    || "customer.subscription.deleted".equals(event.getType())) {
                try {
                    JsonNode root = objectMapper.readTree(payload);
                    JsonNode subNode = root.path("data").path("object");
                    String stripeId = subNode.path("id").asText(null);
                    String stripeStatus = subNode.path("status").asText(null);
                    String uid = subNode.path("metadata").path("app_user_id").asText(null);

                    log.info("customer.subscription.* JSON parsed: id={}, status={}, app_user_id={}", stripeId, stripeStatus, uid);

                    if (stripeId != null && !stripeId.isBlank() && uid != null && !uid.isBlank()) {
                        try {
                            java.util.UUID uuid = java.util.UUID.fromString(uid);
                            User user = userService.getById(uuid);
                            String effectiveStatus = "customer.subscription.deleted".equals(event.getType()) ? "canceled" : stripeStatus;
                            
                            String priceId = subNode.path("items").path("data").get(0).path("price").path("id").asText(null);
                            String planType = resolveSubscriptionPlanType(priceId);
                            
                            subscriptionService.syncProSubscriptionFromStripe(user, stripeId, effectiveStatus, planType);
                        } catch (IllegalArgumentException e) {
                            log.warn("Invalid app_user_id format in subscription JSON: {}", uid);
                        }
                    } else {
                        log.warn("Missing subscription id or app_user_id in subscription JSON");
                    }
                } catch (Exception e) {
                    log.error("Failed to parse customer.subscription.* JSON", e);
                }
            }
            
            return ResponseEntity.ok("Received");
        } catch (Exception e) {
            log.error("Stripe webhook error", e);
            return ResponseEntity.badRequest().body("Webhook Error: " + e.getMessage());
        }
    }
}
