package com.vicinity24.api.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationResponse {
    private String displayName;
    private String streetAddress;
    private String city;
    private String postalCode;
    private String country;
    private String countryCode;
    private Double latitude;
    private Double longitude;
}

