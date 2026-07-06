package com.vicinity24.api.core.dto;

import com.vicinity24.api.core.model.enums.ListingType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CreateListingRequest {
    private String title;
    private String description;
    private String category;
    private ListingType type;
    private BigDecimal hourlyRate;
    private String imageUrl;
    private List<String> gallery;
    private boolean autoApprove;
    private boolean insuranceRequired;
    private Double x;
    private Double y;
    private String streetAddress;
    private String city;
    private String postalCode;
    private String country;
    private java.util.UUID pickupLocationId;
    private String pickupLocationCustom;
    private String pickupLocationStreet;
    private String pickupLocationHouseNumber;
    private String pickupLocationCity;
    private String pickupLocationZip;

    private boolean availableUnlimited;
    private java.time.LocalDateTime availableFrom;
    private java.time.LocalDateTime availableTo;
}
