package com.nearshare.api.dto;

import com.nearshare.api.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private String phone;
    private String address;
    private String avatarUrl;
    private Double lat;
    private Double lng;
    private Boolean isAdmin;
    private UserRole role;
}
