package com.nearshare.api.admin.dto;

import com.nearshare.api.dto.LocationDTO;
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
public class AdminPickupLocationDTO {
    private UUID id;
    private String referenceId;
    private String name;
    private String address;
    private String streetAddress;
    private String city;
    private String postalCode;
    private String country;
    private LocationDTO location;
    private boolean active;
}
