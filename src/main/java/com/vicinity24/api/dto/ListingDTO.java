package com.vicinity24.api.dto;

import com.vicinity24.api.model.enums.AvailabilityStatus;
import com.vicinity24.api.model.enums.ListingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingDTO {
    private UUID id;
    private String itemReference;
    private UUID ownerId;
    private UUID partnerId;
    private String partnerName;
    private String partnerCity;
    private LocalDateTime partnerCreatedAt;
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
    private boolean insuranceRequired;
    private ExchangeLocationDTO pickupLocation;
    private String pickupLocationCustom;
    private String pickupLocationStreet;
    private String pickupLocationHouseNumber;
    private String pickupLocationCity;
    private String pickupLocationZip;

    private boolean availableUnlimited;
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;
}
