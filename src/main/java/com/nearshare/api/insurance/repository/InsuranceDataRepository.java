package com.nearshare.api.insurance.repository;

import com.nearshare.api.insurance.InsuranceType;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for quotes and policies.
 */
@Repository
public class InsuranceDataRepository {
    private final ConcurrentHashMap<String, QuoteRecord> quotes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PolicyRecord> policies = new ConcurrentHashMap<>();

    public QuoteRecord saveQuote(QuoteRecord quote) {
        quotes.put(quote.getQuoteId(), quote);
        return quote;
    }

    public Optional<QuoteRecord> findQuote(String quoteId) {
        if (quoteId == null) return Optional.empty();
        return Optional.ofNullable(quotes.get(quoteId));
    }

    public PolicyRecord savePolicy(PolicyRecord policy) {
        policies.put(policy.getPolicyNumber(), policy);
        return policy;
    }

    public String newQuoteId() {
        return UUID.randomUUID().toString();
    }

    public String newPolicyNumber() {
        return "POL-" + UUID.randomUUID();
    }

    public static class QuoteRecord {
        private String quoteId;
        private String productId;
        private BigDecimal productBasePrice;
        private InsuranceType insuranceType;
        private BigDecimal insuranceCost;
        private BigDecimal totalCost;
        private String currency;
        private OffsetDateTime validUntil;

        public QuoteRecord() {
        }

        public QuoteRecord(String quoteId, String productId, BigDecimal productBasePrice, InsuranceType insuranceType, BigDecimal insuranceCost, BigDecimal totalCost, String currency, OffsetDateTime validUntil) {
            this.quoteId = quoteId;
            this.productId = productId;
            this.productBasePrice = productBasePrice;
            this.insuranceType = insuranceType;
            this.insuranceCost = insuranceCost;
            this.totalCost = totalCost;
            this.currency = currency;
            this.validUntil = validUntil;
        }

        public String getQuoteId() {
            return quoteId;
        }

        public void setQuoteId(String quoteId) {
            this.quoteId = quoteId;
        }

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public BigDecimal getProductBasePrice() {
            return productBasePrice;
        }

        public void setProductBasePrice(BigDecimal productBasePrice) {
            this.productBasePrice = productBasePrice;
        }

        public InsuranceType getInsuranceType() {
            return insuranceType;
        }

        public void setInsuranceType(InsuranceType insuranceType) {
            this.insuranceType = insuranceType;
        }

        public BigDecimal getInsuranceCost() {
            return insuranceCost;
        }

        public void setInsuranceCost(BigDecimal insuranceCost) {
            this.insuranceCost = insuranceCost;
        }

        public BigDecimal getTotalCost() {
            return totalCost;
        }

        public void setTotalCost(BigDecimal totalCost) {
            this.totalCost = totalCost;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public OffsetDateTime getValidUntil() {
            return validUntil;
        }

        public void setValidUntil(OffsetDateTime validUntil) {
            this.validUntil = validUntil;
        }
    }

    public static class PolicyRecord {
        private String policyNumber;
        private String quoteId;
        private String status;
        private OffsetDateTime createdAt;

        public PolicyRecord() {
        }

        public PolicyRecord(String policyNumber, String quoteId, String status, OffsetDateTime createdAt) {
            this.policyNumber = policyNumber;
            this.quoteId = quoteId;
            this.status = status;
            this.createdAt = createdAt;
        }

        public String getPolicyNumber() {
            return policyNumber;
        }

        public void setPolicyNumber(String policyNumber) {
            this.policyNumber = policyNumber;
        }

        public String getQuoteId() {
            return quoteId;
        }

        public void setQuoteId(String quoteId) {
            this.quoteId = quoteId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}

