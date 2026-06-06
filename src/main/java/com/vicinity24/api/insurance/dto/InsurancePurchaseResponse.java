package com.vicinity24.api.insurance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Insurance purchase response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsurancePurchaseResponse {
    private String policyNumber;
    private String status;
    private String message;
}

