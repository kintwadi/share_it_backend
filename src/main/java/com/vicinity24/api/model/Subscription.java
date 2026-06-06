package com.vicinity24.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "plan_type", nullable = false)
    private String planType;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "trial_start")
    private LocalDateTime trialStart;

    @Column(name = "trial_end")
    private LocalDateTime trialEnd;

    @Column(name = "auto_charge_amount_cents")
    private Integer autoChargeAmountCents;

    @Column(name = "auto_charge_date")
    private LocalDateTime autoChargeDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;
}
