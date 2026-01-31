package com.nearshare.api.payment;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class PaymentManager {
    private final PaymentFactory paymentFactory;

    public PaymentManager(PaymentFactory paymentFactory) {
        this.paymentFactory = paymentFactory;
    }

    public boolean processPayment(String method, BigDecimal amount, String currency, String token) {
        PaymentStrategy strategy = paymentFactory.getPaymentStrategy(method);
        return strategy.processPayment(amount, currency, token);
    }
}
