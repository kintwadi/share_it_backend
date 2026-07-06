package com.vicinity24.api.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionDTO {
    private UUID id;
    private String planType;
    private String status;
    private Boolean active;
    private Boolean borrowDirectly;
    private LocalDateTime trialStart;
    private LocalDateTime trialEnd;
    private Integer autoChargeAmountCents;
    private LocalDateTime autoChargeDate;
}
