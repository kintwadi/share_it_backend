package com.nearshare.api.insurance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Insurance quote response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceQuoteResponse {
    private String quoteId;
    private String productId;
    private double productBasePrice;
    private String insuranceType;
    private double insuranceCost;
    private double totalCost;
    private String currency;
    private OffsetDateTime validUntil;
}

