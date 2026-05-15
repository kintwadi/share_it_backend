package com.nearshare.api.insurance.service;

import com.nearshare.api.insurance.InsuranceType;
import com.nearshare.api.insurance.config.InsurancePricingProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculates insurance cost using configured pricing rules.
 */
@Component
public class InsuranceCostCalculator {
    private final InsurancePricingProperties props;

    public InsuranceCostCalculator(InsurancePricingProperties props) {
        this.props = props;
    }

    /**
     * Computes insurance cost for a given type and product price.
     *
     * @param type insurance type
     * @param productBasePrice product price (must be positive)
     * @param customerZipCode optional zip code (may adjust cost)
     * @return insurance cost (2 decimals, half-up)
     */
    public BigDecimal calculate(InsuranceType type, BigDecimal productBasePrice, String customerZipCode) {
        if (productBasePrice == null || productBasePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("invalid_product_price");
        }

        InsurancePricingProperties.Rule rule = ruleFor(type);
        BigDecimal percent = BigDecimal.valueOf(rule.getPercent());
        BigDecimal raw = productBasePrice.multiply(percent);

        BigDecimal min = BigDecimal.valueOf(rule.getMin());
        BigDecimal max = BigDecimal.valueOf(rule.getMax());
        BigDecimal clamped = raw.max(min).min(max);

        BigDecimal adjusted = applyZipAdjustment(clamped, customerZipCode);
        return adjusted.setScale(2, RoundingMode.HALF_UP);
    }

    public String currency() {
        return props.getCurrency();
    }

    public int quoteValidityMinutes() {
        return props.getQuoteValidityMinutes();
    }

    public InsurancePricingProperties.Rule ruleFor(InsuranceType type) {
        return switch (type) {
            case BASIC -> props.getRules().getBasic();
            case PREMIUM -> props.getRules().getPremium();
            case THEFT_PROTECTION -> props.getRules().getTheftProtection();
            case EXTENDED_WARRANTY -> props.getRules().getExtendedWarranty();
        };
    }

    private BigDecimal applyZipAdjustment(BigDecimal cost, String zip) {
        if (zip == null) return cost;
        String trimmed = zip.trim();
        if (trimmed.isEmpty()) return cost;
        String prefix = props.getZipAdjustment().getPrefix();
        if (prefix != null && !prefix.isEmpty() && trimmed.startsWith(prefix)) {
            BigDecimal mult = BigDecimal.valueOf(props.getZipAdjustment().getMultiplier());
            return cost.multiply(mult);
        }
        return cost;
    }
}

