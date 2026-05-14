package com.nearshare.api.insurance.service;

import com.nearshare.api.insurance.config.InsurancePricingProperties;
import com.nearshare.api.insurance.dto.InsurancePurchaseResponse;
import com.nearshare.api.insurance.dto.InsuranceQuoteRequest;
import com.nearshare.api.insurance.exception.InvalidInsuranceTypeException;
import com.nearshare.api.insurance.exception.ResourceNotFoundException;
import com.nearshare.api.insurance.repository.InsuranceDataRepository;
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
        InsurancePricingProperties props = new InsurancePricingProperties();
        props.setCurrency("USD");
        props.setQuoteValidityMinutes(30);

        InsurancePricingProperties.ZipAdjustment adj = new InsurancePricingProperties.ZipAdjustment();
        adj.setPrefix("9");
        adj.setMultiplier(1.15);
        props.setZipAdjustment(adj);

        InsurancePricingProperties.Rule basic = new InsurancePricingProperties.Rule();
        basic.setPercent(0.05);
        basic.setMin(5);
        basic.setMax(50);

        InsurancePricingProperties.Rule premium = new InsurancePricingProperties.Rule();
        premium.setPercent(0.10);
        premium.setMin(10);
        premium.setMax(100);

        InsurancePricingProperties.Rule theft = new InsurancePricingProperties.Rule();
        theft.setPercent(0.08);
        theft.setMin(8);
        theft.setMax(80);

        InsurancePricingProperties.Rule warranty = new InsurancePricingProperties.Rule();
        warranty.setPercent(0.03);
        warranty.setMin(3);
        warranty.setMax(30);

        InsurancePricingProperties.Rules rules = new InsurancePricingProperties.Rules();
        rules.setBasic(basic);
        rules.setPremium(premium);
        rules.setTheftProtection(theft);
        rules.setExtendedWarranty(warranty);
        props.setRules(rules);

        InsuranceCostCalculator calculator = new InsuranceCostCalculator(props);
        InsuranceDataRepository repo = new InsuranceDataRepository();
        return new InsuranceService(calculator, repo);
    }
}

