package com.nearshare.api.insurance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Describes an insurance type and its pricing rule.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceTypeInfoResponse {
    private String insuranceType;
    private double percent;
    private double min;
    private double max;
}

