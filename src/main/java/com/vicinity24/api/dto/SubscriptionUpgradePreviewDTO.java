package com.vicinity24.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionUpgradePreviewDTO {
    private String currentPlan;
    private String newPlan;
    private LocalDate cycleEndDate;
    private int remainingDays;
    
    private int creditCents; // e.g. -139
    private int chargeCents; // e.g. +373
    private int netImmediateChargeCents; // e.g. 234
    
    private int nextFullChargeCents;
    private LocalDate nextFullChargeDate;
}
