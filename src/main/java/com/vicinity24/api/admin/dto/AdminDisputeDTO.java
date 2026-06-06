package com.vicinity24.api.admin.dto;

import com.vicinity24.api.model.enums.AvailabilityStatus;
import com.vicinity24.api.model.enums.ReturnStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminDisputeDTO {
    private UUID listingId;
    private String listingTitle;
    private AvailabilityStatus listingStatus;
    private UUID ownerId;
    private String ownerEmail;
    private UUID borrowerId;
    private String borrowerEmail;
    private UUID returnSessionId;
    private ReturnStatus returnStatus;
    private String disputeReason;
    private LocalDateTime createdAt;
    private UUID latestTransactionId;
    private String latestTransactionStatus;
    private String latestReleaseError;
}

