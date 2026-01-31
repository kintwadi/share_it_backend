package com.nearshare.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRequest {
    private String paymentMethod;
    private String paymentToken;
    private int durationHours;
}
