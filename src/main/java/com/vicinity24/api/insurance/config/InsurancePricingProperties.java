package com.vicinity24.api.insurance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Pricing rules for insurance calculations.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "insurance")
public class InsurancePricingProperties {
    private String currency = "USD";
    private int quoteValidityMinutes = 30;
    private ZipAdjustment zipAdjustment = new ZipAdjustment();
    private Rules rules = new Rules();

    @Getter
    @Setter
    public static class ZipAdjustment {
        private String prefix = "9";
        private double multiplier = 1.15;
    }

    @Getter
    @Setter
    public static class Rule {
        private double percent;
        private double min;
        private double max;
    }

    @Getter
    @Setter
    public static class Rules {
        private Rule basic = new Rule();
        private Rule premium = new Rule();
        private Rule theftProtection = new Rule();
        private Rule extendedWarranty = new Rule();
    }
}

