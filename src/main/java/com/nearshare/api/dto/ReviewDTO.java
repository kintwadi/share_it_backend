package com.nearshare.api.dto;

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
public class ReviewDTO {
    private UUID id;
    private UUID authorId;
    private UUID targetUserId;
    private UUID listingId;
    private int rating;
    private String comment;
    private String timestamp;
}