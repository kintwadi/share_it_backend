package com.vicinity24.api.bicycle.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RentToOwnConversionRequest {
    private UUID borrowerId;
    private String paymentMethod;
    private String paymentToken;
}
