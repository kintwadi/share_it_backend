package com.vicinity24.api.insurance.service;

import com.vicinity24.api.insurance.InsuranceType;
import com.vicinity24.api.config.ConfigProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculates insurance cost using configured pricing rules.
 */
@Component
public class InsuranceCostCalculator {
    private final ConfigProvider config;

    public InsuranceCostCalculator(ConfigProvider config) {
        this.config = config;
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

        Rule rule = ruleFor(type);
        BigDecimal percent = BigDecimal.valueOf(rule.percent);
        BigDecimal raw = productBasePrice.multiply(percent);

        BigDecimal min = BigDecimal.valueOf(rule.min);
        BigDecimal max = BigDecimal.valueOf(rule.max);
        BigDecimal clamped = raw.max(min).min(max);

        BigDecimal adjusted = applyZipAdjustment(clamped, customerZipCode);
        return adjusted.setScale(2, RoundingMode.HALF_UP);
    }

    public String currency() {
        return config.getString("insurance.currency", "USD");
    }

    public int quoteValidityMinutes() {
        return config.getInt("insurance.quote-validity-minutes", 30);
    }

    public Rule ruleFor(InsuranceType type) {
        String base = switch (type) {
            case BASIC -> "basic";
            case PREMIUM -> "premium";
            case THEFT_PROTECTION -> "theft_protection";
            case EXTENDED_WARRANTY -> "extended_warranty";
        };
        double percent = config.getDouble("insurance.rules." + base + ".percent", 0.0);
        double min = config.getDouble("insurance.rules." + base + ".min", 0.0);
        double max = config.getDouble("insurance.rules." + base + ".max", 0.0);
        return new Rule(percent, min, max);
    }

    private BigDecimal applyZipAdjustment(BigDecimal cost, String zip) {
        if (zip == null) return cost;
        String trimmed = zip.trim();
        if (trimmed.isEmpty()) return cost;
        String prefix = config.getString("insurance.zip-adjustment.prefix", "");
        if (prefix != null && !prefix.isEmpty() && trimmed.startsWith(prefix)) {
            BigDecimal mult = BigDecimal.valueOf(config.getDouble("insurance.zip-adjustment.multiplier", 1.0));
            return cost.multiply(mult);
        }
        return cost;
    }

    public static final class Rule {
        public final double percent;
        public final double min;
        public final double max;

        public Rule(double percent, double min, double max) {
            this.percent = percent;
            this.min = min;
            this.max = max;
        }
    }
}
