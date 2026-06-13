package com.vicinity24.api.core.insurance;

import com.vicinity24.api.core.insurance.dto.InsurancePurchaseRequest;
import com.vicinity24.api.core.insurance.dto.InsurancePurchaseResponse;
import com.vicinity24.api.core.insurance.dto.InsuranceQuoteRequest;
import com.vicinity24.api.core.insurance.dto.InsuranceQuoteResponse;
import com.vicinity24.api.core.insurance.dto.InsuranceTypeInfoResponse;
import com.vicinity24.api.core.insurance.service.InsuranceService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for insurance quoting and purchasing.
 */
@RestController
@RequestMapping("/api/insurance")
@ConditionalOnProperty(name = "settings.insurance.enabled", havingValue = "true")
public class InsuranceController {
    private final InsuranceService service;

    public InsuranceController(InsuranceService service) {
        this.service = service;
    }

    @GetMapping("/types")
    public ResponseEntity<List<InsuranceTypeInfoResponse>> types() {
        return ResponseEntity.ok(service.getTypes());
    }

    @PostMapping("/quote")
    public ResponseEntity<InsuranceQuoteResponse> quote(@Valid @RequestBody InsuranceQuoteRequest req) {
        return ResponseEntity.ok(service.quote(req));
    }

    @PostMapping("/purchase")
    public ResponseEntity<InsurancePurchaseResponse> purchase(@Valid @RequestBody InsurancePurchaseRequest req) {
        InsurancePurchaseResponse res = service.purchase(req.getQuoteId());
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }
}

