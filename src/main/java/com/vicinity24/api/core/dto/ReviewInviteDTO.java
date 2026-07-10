package com.vicinity24.api.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewInviteDTO {
    private String token;
    private String listingId;
    private String listingTitle;
    private String listingReference;
    private String reviewerId;
    private String reviewerName;
    private String targetUserId;
    private String targetUserName;
    private boolean used;
    private String expiresAt;
}
