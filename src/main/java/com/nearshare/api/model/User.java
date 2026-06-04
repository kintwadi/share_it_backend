package com.nearshare.api.model;

import com.nearshare.api.model.embeddable.Location;
import com.nearshare.api.model.enums.AdminScope;
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
    private String displayName;
    @Column(unique = true)
    private String email;
    private String password;
    private String phone;
    private String address;
    @Column(length = 2048)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_scope")
    private AdminScope adminScope;

    @Column(name = "two_factor_enabled")
    @Builder.Default
    private Boolean twoFactorEnabled = false;
    
    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_connect_account_id")
    private String stripeConnectAccountId;

    @Column(name = "stripe_connect_details_submitted")
    private Boolean stripeConnectDetailsSubmitted;

    @Column(name = "trust_tier")
    private String trustTier;

    @Column(name = "insurance_coverage_cents")
    private Integer insuranceCoverageCents;

    @Column(name = "profile_visible")
    @Builder.Default
    private Boolean profileVisible = true;

    @Column(name = "show_ratings")
    @Builder.Default
    private Boolean showRatings = true;

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;
}
