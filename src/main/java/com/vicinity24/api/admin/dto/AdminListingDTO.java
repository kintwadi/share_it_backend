package com.vicinity24.api.admin.dto;

import com.vicinity24.api.model.enums.AvailabilityStatus;
import com.vicinity24.api.model.enums.ListingType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminListingDTO {
    private UUID id;
    private String itemReference;
    private String title;
    private ListingType type;
    private AvailabilityStatus status;
    private BigDecimal hourlyRate;
    private UUID ownerId;
    private String ownerEmail;
    private UUID partnerId;
    private String partnerName;
    private UUID borrowerId;
    private String borrowerEmail;
    private LocalDateTime createdAt;

    private boolean availableUnlimited;
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;

    private LocalDateTime partnerSubmittedAt;
    private UUID partnerSubmittedBy;
    private LocalDateTime partnerReviewedAt;
    private UUID partnerReviewedBy;
    private String partnerReviewNote;
    private String partnerRejectionReason;

    private LocalDateTime partnerBorrowRequestedAt;
    private UUID partnerBorrowRequestedBy;
    private LocalDateTime partnerBorrowReviewedAt;
    private UUID partnerBorrowReviewedBy;
    private String partnerBorrowRejectionReason;
}
