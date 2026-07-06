package com.vicinity24.api.core.admin.dto;

import com.vicinity24.api.core.dto.LocationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminExchangeLocationDTO {
    private UUID id;
    private String referenceId;
    private String name;
    private String address;
    private String streetAddress;
    private String city;
    private String postalCode;
    private String country;
    private LocationDTO location;
    private String operatingTimeFrom;
    private String operatingTimeTo;
    private boolean active;
}
