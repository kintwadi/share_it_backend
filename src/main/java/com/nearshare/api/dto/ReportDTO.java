package com.nearshare.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {
    private UUID id;
    private String reason;
    private String details;
    private LocalDateTime timestamp;
    private UserSummaryDTO reporter;
    private ListingDTO listing;
}
