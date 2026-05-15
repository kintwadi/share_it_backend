package com.nearshare.api.insurance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Insurance purchase request.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsurancePurchaseRequest {
    @NotBlank
    private String quoteId;
}

