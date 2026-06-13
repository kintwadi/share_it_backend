package com.vicinity24.api.core.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendSubscriptionCodeRequest {
    private String planType;
    private String language;
}
