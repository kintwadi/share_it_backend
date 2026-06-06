package com.vicinity24.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDTO {
    private String title;
    private String description;
    private LocalDateTime date;
    private String type; // e.g., "account", "listing", "subscription"
}
