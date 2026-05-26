package com.nearshare.api.enterprise.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnterpriseSampleDataDTO {
    private boolean enabled;
    private boolean reset;
    private long existingEnterpriseListings;
    private int partnersCreated;
    private int listingsCreated;
}

