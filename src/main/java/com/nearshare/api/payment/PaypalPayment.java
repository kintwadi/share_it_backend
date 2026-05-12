package com.nearshare.api.payment;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service("paypalPayment")
public class PaypalPayment implements PaymentStrategy {

    @Override
    public boolean processPayment(BigDecimal amount, String currency, String paymentToken) {
        // Mock implementation for PayPal as requested to follow the pattern
        // In a real implementation, this would interact with PayPal SDK
        System.out.println("Processing PayPal payment: " + amount + " " + currency);
        return true;
    }
}
