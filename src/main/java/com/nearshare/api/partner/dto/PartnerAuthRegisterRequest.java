package com.nearshare.api.partner.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartnerAuthRegisterRequest {
    private String userName;
    private String userEmail;
    private String userPassword;
    private PartnerRegistrationRequest partner;
}
