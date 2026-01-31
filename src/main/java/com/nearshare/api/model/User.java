package com.nearshare.api.model;

import com.nearshare.api.model.embeddable.Location;
import com.nearshare.api.model.enums.UserRole;
import com.nearshare.api.model.enums.UserStatus;
import com.nearshare.api.model.enums.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    private UUID id;
    private String name;
    @Column(unique = true)
    private String email;
    private String password;
    private String phone;
    private String address;
    private String avatarUrl;
    private int trustScore;
    private int vouchCount;
    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;
    @Embedded
    private Location location;
    private LocalDateTime joinedDate;
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "two_factor_enabled")
    @Builder.Default
    private Boolean twoFactorEnabled = false;
    
    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;
}