package com.vicinity24.api.core.partner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerSettingsDTO {
    private UUID partnerId;
    private Integer maxLendingDays;
    private Integer depositCents;
    private Boolean autoApproval;
}
