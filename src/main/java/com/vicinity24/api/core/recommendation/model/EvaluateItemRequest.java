package com.vicinity24.api.core.recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluateItemRequest {
    private String title;
    private String category;
    private String description;
    private BigDecimal estimatedValue;
}
