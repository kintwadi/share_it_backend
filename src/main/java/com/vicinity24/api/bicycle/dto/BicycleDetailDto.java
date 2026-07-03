package com.vicinity24.api.bicycle.dto;

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
public class BicycleDetailDto {
    private UUID id;
    private String title;
    private String description;
    private String imageUrl;
    private List<String> gallery;
    private String category;
    private BigDecimal hourlyRate;
    private String city;
    private String country;
    private String frameSize;
    private String bikeType;
    private Integer assemblyBufferMinutes;
    private boolean rentToOwnEligible;
    private BigDecimal retailPurchasePrice;
    private String inventoryStatus;
    private String ownerName;
    private String partnerName;
}
