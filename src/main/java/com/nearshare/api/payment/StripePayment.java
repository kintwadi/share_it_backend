package com.nearshare.api.payment;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.PaymentMethodListParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service("stripePayment")
public class StripePayment implements PaymentStrategy {

    @Value("${STRIPE_SECRET_KEY}")
    private String secretKey;

    @Value("${STRIPE_WEBHOOK_SECRET}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
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
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new RuntimeException("Invalid Webhook Signature", e);
        } catch (Exception e) {
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
}
