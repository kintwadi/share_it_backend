package com.nearshare.api.payment;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Refund;
import com.stripe.model.Transfer;
import com.stripe.net.Webhook;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.PaymentMethodListParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.TransferCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service("stripePayment")
public class StripePayment implements PaymentStrategy {

    private static final Logger log = LoggerFactory.getLogger(StripePayment.class);

    @Value("${STRIPE_SECRET_KEY}")
    private String secretKey;

    @Value("${STRIPE_WEBHOOK_SECRET}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
        try {
            Account acct = Account.retrieve();
            String acctId = acct != null ? acct.getId() : "unknown";
            boolean testMode = secretKey != null && secretKey.startsWith("sk_test_");
            String keyPrefix = secretKey != null && secretKey.length() >= 12 ? secretKey.substring(0, 12) + "..." : "n/a";
            log.info("Stripe initialized: account={}, test={}, keyPrefix={}", acctId, testMode, keyPrefix);
        } catch (Exception e) {
            log.warn("Stripe init check failed: {}", e.getMessage());
        }
    }

    @Override
    public boolean processPayment(BigDecimal amount, String currency, String paymentToken) {
        try {
            // In the new flow, paymentToken is the PaymentIntent ID
            if (paymentToken == null || paymentToken.isEmpty()) {
                return false;
            }
            
            PaymentIntent intent = PaymentIntent.retrieve(paymentToken);
            
            // Verify status
            if (!"succeeded".equals(intent.getStatus())) {
                return false;
            }
            
            // Verify amount (Stripe uses cents)
            long expectedAmount = amount.multiply(new BigDecimal(100)).longValue();
            if (intent.getAmount() != expectedAmount) {
                // Allow small difference for floating point issues? 
                // Better to be strict or allow 1 cent diff.
                // For now, check if difference is > 1 cent
                if (Math.abs(intent.getAmount() - expectedAmount) > 1) {
                    return false;
                }
            }
            
            // Verify currency
            if (!currency.equalsIgnoreCase(intent.getCurrency())) {
                return false;
            }
            
            return true;
        } catch (StripeException e) {
            return false;
        }
    }

    public PaymentIntent createPaymentIntent(BigDecimal amount, String currency, String customerId, java.util.Map<String, String> metadata) {
        try {
            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amount.multiply(new BigDecimal(100)).longValue()) // Amount in cents
                    .setCurrency(currency)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    );

            if (customerId != null && !customerId.isEmpty()) {
                paramsBuilder.setCustomer(customerId);
            }
            
            if (metadata != null) {
                metadata.forEach(paramsBuilder::putMetadata);
            }

            return PaymentIntent.create(paramsBuilder.build());
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create PaymentIntent: " + e.getMessage(), e);
        }
    }

    public Event constructWebhookEvent(String payload, String sigHeader) {
        try {
            boolean configured = webhookSecret != null && !webhookSecret.isBlank();
            if (!configured) {
                throw new RuntimeException("webhook_secret_missing");
            }
            log.debug("Webhook verification - configured={}, sigHeaderPresent={}", configured, sigHeader != null && !sigHeader.isBlank());
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Webhook Signature: {}", e.getMessage());
            throw new RuntimeException("Invalid Webhook Signature", e);
        } catch (Exception e) {
            log.error("Webhook error: {}", e.getMessage());
            throw new RuntimeException("Webhook error", e);
        }
    }

    public String createCustomer(String email, String name) {
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .setName(name)
                    .build();
            Customer customer = Customer.create(params);
            return customer.getId();
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create Stripe customer", e);
        }
    }

    public void attachPaymentMethod(String customerId, String paymentMethodId) {
        try {
            PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
            pm.attach(PaymentMethodAttachParams.builder().setCustomer(customerId).build());
        } catch (StripeException e) {
            throw new RuntimeException("Failed to attach payment method", e);
        }
    }

    public void detachPaymentMethod(String paymentMethodId) {
        try {
            PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
            pm.detach();
        } catch (StripeException e) {
            throw new RuntimeException("Failed to detach payment method", e);
        }
    }

    public List<PaymentMethodDTO> listPaymentMethods(String customerId) {
        try {
            PaymentMethodListParams params = PaymentMethodListParams.builder()
                .setCustomer(customerId)
                .setType(PaymentMethodListParams.Type.CARD)
                .build();
            return PaymentMethod.list(params).getData().stream()
                    .map(pm -> new PaymentMethodDTO(pm.getId(), pm.getCard().getBrand(), pm.getCard().getLast4(), pm.getCard().getExpMonth(), pm.getCard().getExpYear()))
                    .collect(Collectors.toList());
        } catch (StripeException e) {
            throw new RuntimeException("Failed to list payment methods", e);
        }
    }

    public record PaymentMethodDTO(String id, String brand, String last4, Long expMonth, Long expYear) {}

    public Session createSubscriptionCheckoutSession(
            String priceId,
            int trialDays,
            String successUrl,
            String cancelUrl,
            String customerEmail,
            String userId
    ) {
        if (priceId == null || priceId.isEmpty()) {
            throw new IllegalArgumentException("priceId must be configured");
        }

        try {
            SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                    .setPrice(priceId)
                    .setQuantity(1L)
                    .build();

            SessionCreateParams.SubscriptionData.Builder subscriptionDataBuilder = SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("app_user_id", userId);

            if (trialDays > 0) {
                subscriptionDataBuilder.setTrialPeriodDays((long) trialDays);
            }

            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .addLineItem(lineItem)
                    .setSubscriptionData(subscriptionDataBuilder.build())
                    .putMetadata("app_user_id", userId);  // Also set on session for webhook access during checkout

            if (customerEmail != null && !customerEmail.isEmpty()) {
                paramsBuilder.setCustomerEmail(customerEmail);
            }

            return Session.create(paramsBuilder.build());
        } catch (com.stripe.exception.StripeException e) {
            String msg = e.getUserMessage() != null && !e.getUserMessage().isBlank() ? e.getUserMessage() : e.getMessage();
            throw new RuntimeException(msg, e);
        }
    }

    public Session retrieveCheckoutSession(String sessionId) {
        try {
            return Session.retrieve(sessionId);
        } catch (com.stripe.exception.StripeException e) {
            String msg = e.getUserMessage() != null && !e.getUserMessage().isBlank() ? e.getUserMessage() : e.getMessage();
            throw new RuntimeException("Failed to retrieve Stripe session: " + msg, e);
        }
    }

    public void cancelSubscription(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            log.warn("Cannot cancel subscription: subscriptionId is null or blank");
            return;
        }
        
        // Check if Stripe is properly initialized
        if (secretKey == null || secretKey.isBlank()) {
            log.error("Stripe secret key is not configured properly");
            throw new RuntimeException("Stripe secret key is not configured");
        }
        
        try {
            log.info("Attempting to cancel Stripe subscription: {}", subscriptionId);
            com.stripe.model.Subscription subscription = com.stripe.model.Subscription.retrieve(subscriptionId);
            log.info("Retrieved subscription: {}, status: {}", subscription.getId(), subscription.getStatus());
            
            // Cancel the subscription - the cancel() method persists the cancellation to Stripe
            subscription.cancel();
            
            log.info("Successfully canceled Stripe subscription: {}, new status: {}", subscriptionId, subscription.getStatus());
        } catch (com.stripe.exception.StripeException e) {
            log.error("Failed to cancel Stripe subscription: {}, error: {}", subscriptionId, e.getMessage(), e);
            throw new RuntimeException("Failed to cancel Stripe subscription: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error canceling Stripe subscription: {}", subscriptionId, e);
            throw new RuntimeException("Unexpected error canceling Stripe subscription", e);
        }
    }

    public String createExpressConnectAccount(String email, String name) {
        try {
            AccountCreateParams.Builder b = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS);
            if (email != null && !email.isBlank()) {
                b.setEmail(email);
            }
            if (name != null && !name.isBlank()) {
                b.setBusinessProfile(
                        AccountCreateParams.BusinessProfile.builder()
                                .setName(name)
                                .build()
                );
            }
            b.setCapabilities(
                    AccountCreateParams.Capabilities.builder()
                            .setTransfers(AccountCreateParams.Capabilities.Transfers.builder().setRequested(true).build())
                            .build()
            );
            Account acct = Account.create(b.build());
            return acct.getId();
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create connect account: " + e.getMessage(), e);
        }
    }

    public String createAccountOnboardingLink(String accountId, String refreshUrl, String returnUrl) {
        try {
            AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                    .setAccount(accountId)
                    .setRefreshUrl(refreshUrl)
                    .setReturnUrl(returnUrl)
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();
            AccountLink link = AccountLink.create(params);
            return link.getUrl();
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create account link: " + e.getMessage(), e);
        }
    }

    public Account retrieveAccount(String accountId) {
        try {
            return Account.retrieve(accountId);
        } catch (StripeException e) {
            throw new RuntimeException("Failed to retrieve connect account: " + e.getMessage(), e);
        }
    }

    public Transfer createTransfer(BigDecimal amount, String currency, String destinationAccountId, String transferGroup, Map<String, String> metadata) {
        try {
            TransferCreateParams.Builder b = TransferCreateParams.builder()
                    .setAmount(amount.multiply(new BigDecimal(100)).longValue())
                    .setCurrency(currency)
                    .setDestination(destinationAccountId);
            if (transferGroup != null && !transferGroup.isBlank()) {
                b.setTransferGroup(transferGroup);
            }
            if (metadata != null) {
                metadata.forEach(b::putMetadata);
            }
            return Transfer.create(b.build());
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create transfer: " + e.getMessage(), e);
        }
    }

    public Transfer createTransferFromPaymentIntent(BigDecimal amount, String currency, String destinationAccountId, String transferGroup, Map<String, String> metadata, String paymentIntentId) {
        try {
            String effectiveCurrency = currency;
            long requestedCents = amount != null ? amount.multiply(new BigDecimal(100)).longValue() : 0L;
            TransferCreateParams.Builder b = TransferCreateParams.builder()
                    .setAmount(requestedCents)
                    .setDestination(destinationAccountId);

            if (transferGroup != null && !transferGroup.isBlank()) {
                b.setTransferGroup(transferGroup);
            }
            if (metadata != null) {
                metadata.forEach(b::putMetadata);
            }

            if (paymentIntentId != null && !paymentIntentId.isBlank()) {
                PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
                if (intent != null && intent.getCurrency() != null && !intent.getCurrency().isBlank()) {
                    effectiveCurrency = intent.getCurrency();
                }
                String chargeId = intent != null ? intent.getLatestCharge() : null;
                if (chargeId != null && !chargeId.isBlank()) {
                    b.setSourceTransaction(chargeId);
                    com.stripe.model.Charge ch = com.stripe.model.Charge.retrieve(chargeId);
                    String btId = ch != null ? ch.getBalanceTransaction() : null;
                    if (btId != null && !btId.isBlank()) {
                        com.stripe.model.BalanceTransaction bt = com.stripe.model.BalanceTransaction.retrieve(btId);
                        if (bt != null && bt.getCurrency() != null && !bt.getCurrency().isBlank()) {
                            effectiveCurrency = bt.getCurrency();
                        }
                        if (bt != null && bt.getNet() != null) {
                            long maxCents = bt.getNet();
                            if (maxCents > 0 && requestedCents > maxCents) {
                                b.setAmount(maxCents);
                            }
                        }
                    }
                }
            }
            if (effectiveCurrency == null || effectiveCurrency.isBlank()) {
                throw new RuntimeException("missing_currency");
            }
            b.setCurrency(effectiveCurrency);

            return Transfer.create(b.build());
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create transfer: " + e.getMessage(), e);
        }
    }

    public Refund createRefund(String paymentIntentId, BigDecimal amount, Map<String, String> metadata) {
        try {
            RefundCreateParams.Builder b = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .setAmount(amount.multiply(new BigDecimal(100)).longValue());
            if (metadata != null) {
                metadata.forEach(b::putMetadata);
            }
            return Refund.create(b.build());
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create refund: " + e.getMessage(), e);
        }
    }
}
