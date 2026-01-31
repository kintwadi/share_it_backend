package com.nearshare.api.controller;

import com.nearshare.api.payment.StripePayment;
import com.nearshare.api.service.ListingService;
import com.nearshare.api.service.UserService;
import com.nearshare.api.model.User;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final StripePayment stripePayment;
    private final ListingService listingService;
    private final UserService userService;

    public PaymentController(StripePayment stripePayment, ListingService listingService, UserService userService) {
        this.stripePayment = stripePayment;
        this.listingService = listingService;
        this.userService = userService;
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestBody Map<String, Object> payload) {
        
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());
        String currency = (String) payload.get("currency");
        String listingId = (String) payload.get("listingId");
        
        // Optional duration
        String durationHours = payload.containsKey("durationHours") ? payload.get("durationHours").toString() : "0";

        User user = userService.getByEmail(principal.getUsername());
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("listingId", listingId);
        metadata.put("borrowerId", user.getId().toString());
        metadata.put("durationHours", durationHours);

        // We can pass null for customerId for now if we don't have it yet, or use a dummy
        PaymentIntent intent = stripePayment.createPaymentIntent(amount, currency, null, metadata);
        
        return ResponseEntity.ok(Map.of("clientSecret", intent.getClientSecret()));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = stripePayment.constructWebhookEvent(payload, sigHeader);
            
            if ("payment_intent.succeeded".equals(event.getType())) {
                Optional<StripeObject> object = event.getDataObjectDeserializer().getObject();
                if (object.isPresent()) {
                    PaymentIntent intent = (PaymentIntent) object.get();
                    Map<String, String> metadata = intent.getMetadata();
                    
                    if (metadata != null && metadata.containsKey("listingId") && metadata.containsKey("borrowerId")) {
                        String listingId = metadata.get("listingId");
                        String borrowerId = metadata.get("borrowerId");
                        String durationStr = metadata.getOrDefault("durationHours", "0");
                        int duration = Integer.parseInt(durationStr);
                        BigDecimal amount = BigDecimal.valueOf(intent.getAmount()).divide(new BigDecimal(100));
                        
                        listingService.completeTransaction(intent.getId(), listingId, borrowerId, amount, duration);
                    }
                }
            }
            
            return ResponseEntity.ok("Received");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Webhook Error: " + e.getMessage());
        }
    }
}
