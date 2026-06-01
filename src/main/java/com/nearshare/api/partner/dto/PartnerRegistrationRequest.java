package com.nearshare.api.partner.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartnerRegistrationRequest {
    private String name;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String contactPerson;
}
