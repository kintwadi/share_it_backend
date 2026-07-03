package com.vicinity24.api.bicycle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BicycleCatalogItemDto {
    private UUID id;
    private String title;
    private String description;
    private String category;
    private String imageUrl;
    private BigDecimal hourlyRate;
    private String city;
    private String country;
    private String frameSize;
    private String bikeType;
    private Integer assemblyBufferMinutes;
    private boolean rentToOwnEligible;
    private BigDecimal retailPurchasePrice;
    private String inventoryStatus;
    private LocalDateTime createdAt;
}
