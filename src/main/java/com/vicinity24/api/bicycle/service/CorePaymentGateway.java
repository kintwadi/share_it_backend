package com.vicinity24.api.bicycle.service;

import com.vicinity24.api.core.model.Transaction;
import com.vicinity24.api.core.payment.PaymentManager;
import com.vicinity24.api.core.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CorePaymentGateway {

    private final TransactionRepository transactionRepository;
    private final PaymentManager paymentManager;

    public CorePaymentGateway(TransactionRepository transactionRepository, PaymentManager paymentManager) {
        this.transactionRepository = transactionRepository;
        this.paymentManager = paymentManager;
    }

    public BigDecimal getTotalPaidForListing(UUID listingId, UUID borrowerId) {
        List<Transaction> transactions = transactionRepository.findByListingId(listingId);
        return transactions.stream()
                .filter(tx -> borrowerId == null || (tx.getPayer() != null && borrowerId.equals(tx.getPayer().getId())))
                .filter(tx -> tx.getStatus() == null || !"FAILED".equalsIgnoreCase(tx.getStatus()))
                .map(tx -> tx.getRentalAmount() != null ? tx.getRentalAmount() : tx.getAmount())
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean executeCharge(BigDecimal amount, String currency, String paymentMethod, String paymentToken) {
        return paymentManager.processPayment(
                paymentMethod == null || paymentMethod.isBlank() ? "CARD" : paymentMethod,
                amount,
                currency == null || currency.isBlank() ? "EUR" : currency,
                paymentToken
        );
    }
}
