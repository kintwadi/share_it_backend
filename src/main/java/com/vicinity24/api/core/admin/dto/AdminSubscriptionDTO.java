package com.vicinity24.api.core.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminSubscriptionDTO {
    private UUID id;
    private UUID userId;
    private String userEmail;
    private String planType;
    private String status;
    private LocalDateTime trialStart;
    private LocalDateTime trialEnd;
    private Integer autoChargeAmountCents;
    private LocalDateTime autoChargeDate;
    private LocalDateTime createdAt;
    private String stripeSubscriptionId;
}

