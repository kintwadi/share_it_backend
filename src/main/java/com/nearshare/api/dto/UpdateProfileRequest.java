package com.nearshare.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@lombok.ToString
public class UpdateProfileRequest {
    private String name;
    @JsonProperty("displayName")
    private String displayName;
    private String avatarUrl;
    private String phone;
    private String address;
    private Boolean profileVisible;
    private Boolean showRatings;
}