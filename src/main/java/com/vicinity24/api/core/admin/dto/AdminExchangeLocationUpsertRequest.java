package com.vicinity24.api.core.admin.dto;

import lombok.Data;

@Data
public class AdminExchangeLocationUpsertRequest {
    private String name;
    private String address;
    private String streetAddress;
    private String city;
    private String postalCode;
    private String country;
    private Double latitude;
    private Double longitude;
    private String operatingTimeFrom;
    private String operatingTimeTo;
    private Boolean active;
}
