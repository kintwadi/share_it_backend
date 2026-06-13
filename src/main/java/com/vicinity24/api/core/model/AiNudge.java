package com.vicinity24.api.core.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_nudges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiNudge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "nudge_type", nullable = false)
    private String nudgeType; // give_to_lend, give_to_sell, lend_to_sell, hidden_inventory

    @Column(name = "demand_score")
    private BigDecimal demandScore;

    @Column(name = "status", nullable = false)
    private String status; // sent, accepted, declined, ignored

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "response_action")
    private String responseAction;
}
