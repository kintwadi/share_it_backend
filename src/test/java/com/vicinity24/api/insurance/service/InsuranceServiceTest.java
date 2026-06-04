package com.vicinity24.api.insurance.service;

import com.vicinity24.api.config.ConfigProvider;
import com.vicinity24.api.insurance.dto.InsurancePurchaseResponse;
import com.vicinity24.api.insurance.dto.InsuranceQuoteRequest;
import com.vicinity24.api.insurance.exception.InvalidInsuranceTypeException;
import com.vicinity24.api.insurance.exception.ResourceNotFoundException;
import com.vicinity24.api.insurance.repository.InsuranceDataRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InsuranceServiceTest {

    @Test
    public void quote_shouldComputeTotalCost() {
        InsuranceService service = newService();

        InsuranceQuoteRequest req = new InsuranceQuoteRequest();
        req.setProductId("prod-1");
        req.setProductBasePrice(200.0);
        req.setInsuranceType("basic");
        req.setCustomerZipCode("10001");

        var res = service.quote(req);
        Assertions.assertNotNull(res.getQuoteId());
        Assertions.assertEquals("prod-1", res.getProductId());
        Assertions.assertEquals("BASIC", res.getInsuranceType());
        Assertions.assertEquals(210.0, res.getTotalCost(), 0.001);
        Assertions.assertEquals("USD", res.getCurrency());
        Assertions.assertNotNull(res.getValidUntil());
    }

    @Test
    public void quote_shouldApplyZipAdjustment() {
        InsuranceService service = newService();

        InsuranceQuoteRequest req = new InsuranceQuoteRequest();
        req.setProductId("prod-2");
        req.setProductBasePrice(100.0);
        req.setInsuranceType("premium");
        req.setCustomerZipCode("90001");

        var res = service.quote(req);
        Assertions.assertEquals("PREMIUM", res.getInsuranceType());
        Assertions.assertEquals(11.5, res.getInsuranceCost(), 0.001);
        Assertions.assertEquals(111.5, res.getTotalCost(), 0.001);
    }

    @Test
    public void purchase_shouldCreatePolicy() {
        InsuranceService service = newService();

        InsuranceQuoteRequest req = new InsuranceQuoteRequest();
        req.setProductId("prod-3");
        req.setProductBasePrice(50.0);
        req.setInsuranceType("extended warranty");

        var quote = service.quote(req);
        InsurancePurchaseResponse purchase = service.purchase(quote.getQuoteId());

        Assertions.assertNotNull(purchase.getPolicyNumber());
        Assertions.assertEquals("ACTIVE", purchase.getStatus());
        Assertions.assertEquals("policy_created", purchase.getMessage());
    }

    @Test
    public void purchase_unknownQuote_shouldThrow404() {
        InsuranceService service = newService();
        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.purchase("missing"));
    }

    @Test
    public void quote_invalidType_shouldThrow400() {
        InsuranceService service = newService();

        InsuranceQuoteRequest req = new InsuranceQuoteRequest();
        req.setProductId("prod-4");
        req.setProductBasePrice(50.0);
        req.setInsuranceType("not-a-type");

        Assertions.assertThrows(InvalidInsuranceTypeException.class, () -> service.quote(req));
    }

    private static InsuranceService newService() {
        ConfigProvider config = new ConfigProvider() {
            @Override
            public String getString(String key, String defaultValue) {
                if ("insurance.currency".equals(key)) return "USD";
                if ("insurance.zip-adjustment.prefix".equals(key)) return "9";
                return defaultValue;
            }

            @Override
            public int getInt(String key, int defaultValue) {
                if ("insurance.quote-validity-minutes".equals(key)) return 30;
                return defaultValue;
            }

            @Override
            public double getDouble(String key, double defaultValue) {
                return switch (key) {
                    case "insurance.zip-adjustment.multiplier" -> 1.15;
                    case "insurance.rules.basic.percent" -> 0.05;
                    case "insurance.rules.basic.min" -> 5;
                    case "insurance.rules.basic.max" -> 50;
                    case "insurance.rules.premium.percent" -> 0.10;
                    case "insurance.rules.premium.min" -> 10;
                    case "insurance.rules.premium.max" -> 100;
                    case "insurance.rules.theft_protection.percent" -> 0.08;
                    case "insurance.rules.theft_protection.min" -> 8;
                    case "insurance.rules.theft_protection.max" -> 80;
                    case "insurance.rules.extended_warranty.percent" -> 0.03;
                    case "insurance.rules.extended_warranty.min" -> 3;
                    case "insurance.rules.extended_warranty.max" -> 30;
                    default -> defaultValue;
                };
            }

            @Override
            public boolean getBoolean(String key, boolean defaultValue) {
                return defaultValue;
            }
        };

        InsuranceCostCalculator calculator = new InsuranceCostCalculator(config);
        InsuranceDataRepository repo = new InsuranceDataRepository();
        return new InsuranceService(calculator, repo);
    }
}

