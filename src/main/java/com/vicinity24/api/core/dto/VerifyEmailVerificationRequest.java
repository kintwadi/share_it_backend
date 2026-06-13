package com.vicinity24.api.core.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailVerificationRequest {
    private String token;
    private String code;
}

