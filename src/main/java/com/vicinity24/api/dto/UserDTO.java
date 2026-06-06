package com.vicinity24.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vicinity24.api.model.enums.AdminScope;
import com.vicinity24.api.model.enums.UserRole;
import com.vicinity24.api.model.enums.VerificationStatus;
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
    @JsonProperty("displayName")
    private String displayName;
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
    private Boolean profileVisible;
    private Boolean showRatings;
    private AdminScope adminScope;
    private Boolean emailVerified;
}
