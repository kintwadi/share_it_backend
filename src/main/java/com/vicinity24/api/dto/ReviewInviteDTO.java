package com.vicinity24.api.dto;

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
    private String reviewerId;
    private String targetUserId;
    private String targetUserName;
    private boolean used;
    private String expiresAt;
}

