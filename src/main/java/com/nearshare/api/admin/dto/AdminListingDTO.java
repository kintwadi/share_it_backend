package com.nearshare.api.admin.dto;

import com.nearshare.api.model.enums.AvailabilityStatus;
import com.nearshare.api.model.enums.ListingType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminListingDTO {
    private UUID id;
    private String title;
    private ListingType type;
    private AvailabilityStatus status;
    private BigDecimal hourlyRate;
    private UUID ownerId;
    private String ownerEmail;
    private UUID borrowerId;
    private String borrowerEmail;
    private LocalDateTime createdAt;
}

