package com.vicinity24.api.core.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "review_invites",
        indexes = {
                @Index(name = "idx_review_invites_token", columnList = "token"),
                @Index(name = "idx_review_invites_session", columnList = "returnSessionId")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_review_invites_token", columnNames = {"token"}),
                @UniqueConstraint(name = "uq_review_invites_session_reviewer_target", columnNames = {"returnSessionId", "reviewer_id", "target_user_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewInvite {
    @Id
    private UUID id;
    private String token;
    private UUID returnSessionId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    private Listing listing;
}

