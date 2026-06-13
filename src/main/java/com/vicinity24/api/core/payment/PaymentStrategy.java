package com.vicinity24.api.core.payment;

import java.math.BigDecimal;

public interface PaymentStrategy {
    boolean processPayment(BigDecimal amount, String currency, String paymentToken);
}
