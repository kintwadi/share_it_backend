package com.nearshare.api.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminRegisterRequest {
    private String name;
    private String email;
    private String password;
    private String signupSecret;
}
