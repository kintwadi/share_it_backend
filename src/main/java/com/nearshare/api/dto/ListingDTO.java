package com.nearshare.api.dto;

import com.nearshare.api.model.enums.AvailabilityStatus;
import com.nearshare.api.model.enums.ListingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingDTO {
    private UUID id;
    private UUID ownerId;
    private UUID borrowerId;
    private String title;
    private String description;
    private ListingType type;
    private String category;
    private String imageUrl;
    private double distanceMiles;
    private AvailabilityStatus status;
    private BigDecimal hourlyRate;
    private LocationDTO location;
    private UserSummaryDTO owner;
    private UserSummaryDTO borrower;
    private List<String> gallery;
    private boolean autoApprove;
}