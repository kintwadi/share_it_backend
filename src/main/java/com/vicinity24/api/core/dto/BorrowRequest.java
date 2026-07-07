package com.vicinity24.api.core.dto;

import com.vicinity24.api.core.model.enums.PricingUnit;
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
    private int durationValue;
    private PricingUnit durationUnit;
    private String borrowerPath;
}
