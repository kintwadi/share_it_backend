package com.nearshare.api.payment;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentFactory {
    
    private final Map<String, PaymentStrategy> strategies;

    public PaymentFactory(Map<String, PaymentStrategy> strategies) {
        this.strategies = strategies;
    }

    public PaymentStrategy getPaymentStrategy(String method) {
        String beanName = method.toLowerCase() + "Payment";
        PaymentStrategy strategy = strategies.get(beanName);
        if (strategy == null) {
            throw new IllegalArgumentException("Invalid payment method: " + method);
        }
        return strategy;
    }
}
