package com.vicinity24.api.insurance.service;

import com.vicinity24.api.insurance.InsuranceType;
import com.vicinity24.api.insurance.dto.InsurancePurchaseResponse;
import com.vicinity24.api.insurance.dto.InsuranceQuoteRequest;
import com.vicinity24.api.insurance.dto.InsuranceQuoteResponse;
import com.vicinity24.api.insurance.dto.InsuranceTypeInfoResponse;
import com.vicinity24.api.insurance.exception.ResourceNotFoundException;
import com.vicinity24.api.insurance.repository.InsuranceDataRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Insurance business logic for quotes and purchases.
 */
@Service
public class InsuranceService {
    private final InsuranceCostCalculator calculator;
    private final InsuranceDataRepository repo;

    public InsuranceService(InsuranceCostCalculator calculator, InsuranceDataRepository repo) {
        this.calculator = calculator;
        this.repo = repo;
    }

    public List<InsuranceTypeInfoResponse> getTypes() {
        return List.of(
                toInfo(InsuranceType.BASIC),
                toInfo(InsuranceType.PREMIUM),
                toInfo(InsuranceType.THEFT_PROTECTION),
                toInfo(InsuranceType.EXTENDED_WARRANTY)
        );
    }

    public InsuranceQuoteResponse quote(InsuranceQuoteRequest req) {
        InsuranceType type = InsuranceType.fromString(req.getInsuranceType());
        BigDecimal base = BigDecimal.valueOf(req.getProductBasePrice());
        BigDecimal insuranceCost = calculator.calculate(type, base, req.getCustomerZipCode());
        BigDecimal total = base.add(insuranceCost);

        String quoteId = repo.newQuoteId();
        OffsetDateTime validUntil = OffsetDateTime.now().plusMinutes(calculator.quoteValidityMinutes());

        repo.saveQuote(new InsuranceDataRepository.QuoteRecord(
                quoteId,
                req.getProductId(),
                base,
                type,
                insuranceCost,
                total,
                calculator.currency(),
                validUntil
        ));

        return new InsuranceQuoteResponse(
                quoteId,
                req.getProductId(),
                base.doubleValue(),
                type.name(),
                insuranceCost.doubleValue(),
                total.doubleValue(),
                calculator.currency(),
                validUntil
        );
    }

    public InsurancePurchaseResponse purchase(String quoteId) {
        var quote = repo.findQuote(quoteId).orElseThrow(() -> new ResourceNotFoundException("quote_not_found"));
        if (quote.getValidUntil() != null && quote.getValidUntil().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("quote_expired");
        }

        String policyNumber = repo.newPolicyNumber();
        repo.savePolicy(new InsuranceDataRepository.PolicyRecord(policyNumber, quoteId, "ACTIVE", OffsetDateTime.now()));

        return new InsurancePurchaseResponse(policyNumber, "ACTIVE", "policy_created");
    }

    private InsuranceTypeInfoResponse toInfo(InsuranceType type) {
        var r = calculator.ruleFor(type);
        return new InsuranceTypeInfoResponse(type.name(), r.percent, r.min, r.max);
    }
}
