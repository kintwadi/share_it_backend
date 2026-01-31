package com.nearshare.api.dto;

import com.nearshare.api.model.enums.ListingType;
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
    private Double x;
    private Double y;
}