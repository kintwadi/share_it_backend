package com.nearshare.api.payment;

import java.math.BigDecimal;

public interface PaymentStrategy {
    boolean processPayment(BigDecimal amount, String currency, String paymentToken);
}
