package com.vicinity24.api.core.admin.dto;

import com.vicinity24.api.core.model.enums.UserRole;
import com.vicinity24.api.core.model.enums.UserStatus;
import com.vicinity24.api.core.model.enums.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminUserDTO {
    private UUID id;
    private String name;
    private String displayName;
    private String email;
    private String phone;
    private String address;
    private UserRole role;
    private UserStatus status;
    private VerificationStatus verificationStatus;
    private Integer trustScore;
    private Integer vouchCount;
    private LocalDateTime joinedDate;
    private Boolean twoFactorEnabled;
}

