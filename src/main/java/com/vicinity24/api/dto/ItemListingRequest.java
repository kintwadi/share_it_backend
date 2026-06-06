package com.vicinity24.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemListingRequest {
    private String title;
    private String streetAddress;
    private String city;
    private String postalCode;
    private String country;
}

