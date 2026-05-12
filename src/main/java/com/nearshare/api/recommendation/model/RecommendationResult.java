package com.nearshare.api.recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResult {
    private String recommendedAction; // "SELL", "LEND", "GIVE"
    private BigDecimal suggestedPrice;
    private double confidenceScore; // 0.0 to 1.0
    private String reasoning;
    private List<SimilarItem> similarItems;
}
