package com.nearshare.api.dto;

import com.nearshare.api.model.enums.UserRole;
import com.nearshare.api.model.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private UUID id;
    private String name;
    private String email;
    private UserRole role;
    private String avatarUrl;
    private int trustScore;
    private int vouchCount;
    private VerificationStatus verificationStatus;
    private LocationDTO location;
    private String joinedDate;
    private String phone;
    private String address;
    private boolean twoFactorEnabled;
}