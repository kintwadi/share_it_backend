package com.nearshare.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    private String name;
    private String avatarUrl;
    private String phone;
    private String address;
}