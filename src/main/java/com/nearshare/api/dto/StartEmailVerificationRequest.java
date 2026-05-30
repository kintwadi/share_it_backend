package com.nearshare.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartEmailVerificationRequest {
    private String email;
    private String language;
}

