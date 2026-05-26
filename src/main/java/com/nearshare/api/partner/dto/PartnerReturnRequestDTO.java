package com.nearshare.api.partner.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PartnerReturnRequestDTO {
    private UUID listingId;
    private String listingTitle;
    private String itemReference;
    private UUID partnerId;
    private String partnerName;
    private UUID borrowerId;
    private String borrowerName;
    private String borrowerEmail;
    private LocalDateTime borrowerConfirmedAt;
}
