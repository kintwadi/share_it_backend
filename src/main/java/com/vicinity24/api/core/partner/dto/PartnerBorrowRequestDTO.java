package com.vicinity24.api.core.partner.dto;

import com.vicinity24.api.core.model.enums.AvailabilityStatus;
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
public class PartnerBorrowRequestDTO {
    private UUID listingId;
    private String listingTitle;
    private UUID partnerId;
    private String partnerName;
    private UUID borrowerId;
    private String borrowerName;
    private String borrowerEmail;
    private AvailabilityStatus status;
}
