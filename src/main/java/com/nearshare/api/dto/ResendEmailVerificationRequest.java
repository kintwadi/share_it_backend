package com.nearshare.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendEmailVerificationRequest {
    private String token;
    private String language;
}

