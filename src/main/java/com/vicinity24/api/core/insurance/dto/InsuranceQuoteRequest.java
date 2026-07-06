package com.vicinity24.api.core.insurance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Insurance quote request.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceQuoteRequest {
    @NotBlank
    private String productId;

    @NotNull
    @Positive
    private Double productBasePrice;

    @NotBlank
    private String insuranceType;

    private String customerZipCode;
}

